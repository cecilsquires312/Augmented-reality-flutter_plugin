package io.carius.lars.ar_flutter_plugin

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.PlaneRenderer
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Quaternion
import io.carius.lars.ar_flutter_plugin.Serialization.deserializeMatrix4
import io.carius.lars.ar_flutter_plugin.Serialization.serializeHitResult
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.loader.FlutterLoader
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import java.nio.FloatBuffer
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

internal class AndroidARView(
        val activity: Activity,
        context: Context,
        messenger: BinaryMessenger,
        id: Int,
        creationParams: Map<String?, Any?>?
) : PlatformView {
    // constants
    private val TAG: String = AndroidARView::class.java.name
    // Lifecycle variables
    private var mUserRequestedInstall = true
    lateinit var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks
    private val viewContext: Context
    // Platform channels
    private val sessionManagerChannel: MethodChannel = MethodChannel(messenger, "arsession_$id")
    private val objectManagerChannel: MethodChannel = MethodChannel(messenger, "arobjects_$id")
    private val anchorManagerChannel: MethodChannel = MethodChannel(messenger, "aranchors_$id")
    // UI variables
    private lateinit var arSceneView: ArSceneView
    private var showFeaturePoints = false
    private var pointCloudNode = Node()
    private var worldOriginNode = Node()
    // Model builder
    private var modelBuilder = ArModelBuilder()

    // Method channel handlers
    private val onSessionMethodCall =
            object : MethodChannel.MethodCallHandler {
                override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
                    Log.d(TAG, "AndroidARView onsessionmethodcall reveived a call!")
                    when (call.method) {
                        "init" -> {
                            initializeARView(call, result)
                        }
                        else -> {}
                    }
                }
            }
    private val onObjectMethodCall =
            object : MethodChannel.MethodCallHandler {
                override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
                    Log.d(TAG, "AndroidARView onobjectmethodcall reveived a call!")
                    when (call.method) {
                        "init" -> {
                            // objectManagerChannel.invokeMethod("onError", listOf("ObjectTEST from
                            // Android"))
                        }
                        "addNode" -> {
                            val dict_node: HashMap<String, Any>? = call.arguments as? HashMap<String, Any>
                            dict_node?.let{
                                addNode(it).thenAccept{status: Boolean ->
                                    result.success(status)
                                }.exceptionally { throwable ->
                                    result.error(null, throwable.message, throwable.stackTrace)
                                    null
                                }
                            }
                        }
                        "addNodeToPlaneAnchor" -> {
                            val dict_node: HashMap<String, Any>? = call.argument<HashMap<String, Any>>("node")
                            val dict_anchor: HashMap<String, Any>? = call.argument<HashMap<String, Any>>("anchor")
                            if (dict_node != null && dict_anchor != null) {
                                addNode(dict_node, dict_anchor).thenAccept{status: Boolean ->
                                    result.success(status)
                                }.exceptionally { throwable ->
                                    result.error(null, throwable.message, throwable.stackTrace)
                                    null
                                }
                            } else {
                                result.success(false)
                            }

                        }
                        "removeNode" -> {
                            val nodeName: String? = call.argument<String>("name")
                            nodeName?.let{
                                val node = arSceneView.scene.findByName(nodeName)
                                node?.let{
                                    arSceneView.scene.removeChild(node)
                                    result.success(null)
                                }
                            }
                        }
                        "transformationChanged" -> {
                            val nodeName: String? = call.argument<String>("name")
                            val newTransformation: ArrayList<Double>? = call.argument<ArrayList<Double>>("transformation")
                            nodeName?.let{ name ->
                                newTransformation?.let{ transform ->
                                    transformNode(name, transform)
                                    result.success(null)
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
    private val onAnchorMethodCall =
            object : MethodChannel.MethodCallHandler {
                override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
                    when (call.method) {
                        "addAnchor" -> {
                            val anchorType: Int? = call.argument<Int>("type")
                            anchorType?.let{
                                when(it) {
                                    0 -> { // Plane Anchor
                                        val transform: ArrayList<Double>? = call.argument<ArrayList<Double>>("transformation")
                                        val name: String? = call.argument<String>("name")
                                        if ( name != null && transform != null){
                                            result.success(addPlaneAnchor(transform, name))
                                        } else {
                                            result.success(false)
                                        }

                                    }
                                    else -> result.success(false)
                                }

                            }
                            result.success(false)
                        }
                        "removeAnchor" -> {
                            val anchorName: String? = call.argument<String>("name")
                            anchorName?.let{ name ->
                                removeAnchor(name)
                            }
                        }
                        else -> {}
                    }
                }
            }

    override fun getView(): View {
        return arSceneView
    }

    override fun dispose() {
        // Destroy AR session
        Log.d(TAG, "dispose called")
        try {
            onPause()
            arSceneView.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    init {

        Log.d(TAG, "Initializing AndroidARView")
        viewContext = context

        arSceneView = ArSceneView(context)

        setupLifeCycle(context)

        sessionManagerChannel.setMethodCallHandler(onSessionMethodCall)
        objectManagerChannel.setMethodCallHandler(onObjectMethodCall)
        anchorManagerChannel.setMethodCallHandler(onAnchorMethodCall)

        onResume() // call onResume once to setup initial session
        // TODO: find out why this does not happen automatically
    }

    private fun setupLifeCycle(context: Context) {
        activityLifecycleCallbacks =
                object : Application.ActivityLifecycleCallbacks {
                    override fun onActivityCreated(
                            activity: Activity,
                            savedInstanceState: Bundle?
                    ) {
                        Log.d(TAG, "onActivityCreated")
                    }

                    override fun onActivityStarted(activity: Activity) {
                        Log.d(TAG, "onActivityStarted")
                    }

                    override fun onActivityResumed(activity: Activity) {
                        Log.d(TAG, "onActivityResumed")
                        onResume()
                    }

                    override fun onActivityPaused(activity: Activity) {
                        Log.d(TAG, "onActivityPaused")
                        onPause()
                    }

                    override fun onActivityStopped(activity: Activity) {
                        Log.d(TAG, "onActivityStopped")
                        // onStopped()
                    }

                    override fun onActivitySaveInstanceState(
                            activity: Activity,
                            outState: Bundle
                    ) {}

                    override fun onActivityDestroyed(activity: Activity) {
                        Log.d(TAG, "onActivityDestroyed")
                        // onDestroy()
                    }
                }

        activity.application.registerActivityLifecycleCallbacks(this.activityLifecycleCallbacks)
    }

    fun onResume() {
        // Create session if there is none
        if (arSceneView.session == null) {
            Log.d(TAG, "ARSceneView session is null. Trying to initialize")
            try {
                var session: Session?
                if (ArCoreApk.getInstance().requestInstall(activity, mUserRequestedInstall) ==
                        ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                    Log.d(TAG, "Install of ArCore APK requested")
                    session = null
                } else {
                    session = Session(activity)
                }

                if (session == null) {
                    // Ensures next invocation of requestInstall() will either return
                    // INSTALLED or throw an exception.
                    mUserRequestedInstall = false
                    return
                } else {
                    val config = Config(session)
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    config.focusMode = Config.FocusMode.AUTO
                    session.configure(config)
                    arSceneView.setupSession(session)
                }
            } catch (ex: UnavailableUserDeclinedInstallationException) {
                // Display an appropriate message to the user zand return gracefully.
                Toast.makeText(
                                activity,
                                "TODO: handle exception " + ex.localizedMessage,
                                Toast.LENGTH_LONG)
                        .show()
                return
            } catch (ex: UnavailableArcoreNotInstalledException) {
                Toast.makeText(activity, "Please install ARCore", Toast.LENGTH_LONG).show()
                return
            } catch (ex: UnavailableApkTooOldException) {
                Toast.makeText(activity, "Please update ARCore", Toast.LENGTH_LONG).show()
                return
            } catch (ex: UnavailableSdkTooOldException) {
                Toast.makeText(activity, "Please update this app", Toast.LENGTH_LONG).show()
                return
            } catch (ex: UnavailableDeviceNotCompatibleException) {
                Toast.makeText(activity, "This device does not support AR", Toast.LENGTH_LONG)
                        .show()
                return
            } catch (e: Exception) {
                Toast.makeText(activity, "Failed to create AR session", Toast.LENGTH_LONG).show()
                return
            }
        }

        try {
            arSceneView.resume()
        } catch (ex: CameraNotAvailableException) {
            Log.d(TAG, "Unable to get camera" + ex)
            activity.finish()
            return
        }
    }

    fun onPause() {
        arSceneView.pause()
    }

    private fun initializeARView(call: MethodCall, result: MethodChannel.Result) {
        // Unpack call arguments
        val argShowFeaturePoints: Boolean? = call.argument<Boolean>("showFeaturePoints")
        val argPlaneDetectionConfig: Int? = call.argument<Int>("planeDetectionConfig")
        val argShowPlanes: Boolean? = call.argument<Boolean>("showPlanes")
        val argCustomPlaneTexturePath: String? = call.argument<String>("customPlaneTexturePath")
        val argShowWorldOrigin: Boolean? = call.argument<Boolean>("showWorldOrigin")
        val argHandleTaps: Boolean? = call.argument<Boolean>("handleTaps")

        arSceneView.scene.addOnUpdateListener { frameTime: FrameTime -> onFrame(frameTime) }

        // Configure feature points
        if (argShowFeaturePoints ==
                true) { // explicit comparison necessary because of nullable type
            arSceneView.scene.addChild(pointCloudNode)
            showFeaturePoints = true
        } else {
            showFeaturePoints = false
            while (pointCloudNode.children?.size
                    ?: 0 > 0) {
                pointCloudNode.children?.first()?.setParent(null)
            }
            pointCloudNode.setParent(null)
        }

        // Configure plane detection
        val config = arSceneView.session?.config
        if (config == null) {
            sessionManagerChannel.invokeMethod("onError", listOf("session is null"))
        }
        when (argPlaneDetectionConfig) {
            1 -> {
                config?.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
            }
            2 -> {
                config?.planeFindingMode = Config.PlaneFindingMode.VERTICAL
            }
            3 -> {
                config?.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            }
            else -> {
                config?.planeFindingMode = Config.PlaneFindingMode.DISABLED
            }
        }
        arSceneView.session?.configure(config)

        // Configure whether or not detected planes should be shown
        arSceneView.planeRenderer.isVisible = if (argShowPlanes == true) true else false
        // Create custom plane renderer (use supplied texture & increase radius)
        argCustomPlaneTexturePath?.let {
            val loader: FlutterLoader = FlutterInjector.instance().flutterLoader()
            val key: String = loader.getLookupKeyForAsset(it)

            val sampler =
                    Texture.Sampler.builder()
                            .setMinFilter(Texture.Sampler.MinFilter.LINEAR)
                            .setWrapMode(Texture.Sampler.WrapMode.REPEAT)
                            .build()
            Texture.builder()
                    .setSource(viewContext, Uri.parse(key))
                    .setSampler(sampler)
                    .build()
                    .thenAccept { texture: Texture? ->
                        arSceneView.planeRenderer.material.thenAccept { material: Material ->
                            material.setTexture(PlaneRenderer.MATERIAL_TEXTURE, texture)
                            material.setFloat(PlaneRenderer.MATERIAL_SPOTLIGHT_RADIUS, 10f)
                        }
                    }
            // Set radius to render planes in
            arSceneView.scene.addOnUpdateListener { frameTime: FrameTime? ->
                val planeRenderer = arSceneView.planeRenderer
                planeRenderer.material.thenAccept { material: Material ->
                    material.setFloat(
                            PlaneRenderer.MATERIAL_SPOTLIGHT_RADIUS,
                            10f) // Sets the radius in which to visualize planes
                }
            }
        }

        // Configure world origin
        if (argShowWorldOrigin == true) {
            worldOriginNode = modelBuilder.makeWorldOriginNode(viewContext)
            arSceneView.scene.addChild(worldOriginNode)
        } else {
            worldOriginNode.setParent(null)
        }

        // Configure Tap handling
        if (argHandleTaps == true) { // explicit comparison necessary because of nullable type
            arSceneView.scene.setOnTouchListener{ hitTestResult: HitTestResult, motionEvent: MotionEvent? -> onTap(hitTestResult, motionEvent) }
        }

        result.success(null)
    }

    private fun onFrame(frameTime: FrameTime) {
        if (showFeaturePoints) {
            // remove points from last frame
            while (pointCloudNode.children?.size
                    ?: 0 > 0) {
                pointCloudNode.children?.first()?.setParent(null)
            }
            var pointCloud = arSceneView.arFrame?.acquirePointCloud()
            // Access point cloud data (returns FloatBufferw with x,y,z coordinates and confidence
            // value).
            val points = pointCloud?.getPoints() ?: FloatBuffer.allocate(0)
            // Check if there are any feature points
            if (points.limit() / 4 >= 1) {
                for (index in 0 until points.limit() / 4) {
                    // Add feature point to scene
                    val featurePoint =
                            modelBuilder.makeFeaturePointNode(
                                    viewContext,
                                    points.get(4 * index),
                                    points.get(4 * index + 1),
                                    points.get(4 * index + 2))
                    featurePoint.setParent(pointCloudNode)
                }
            }
            // Release resources
            pointCloud?.release()
        }
    }

    private fun addNode(dict_node: HashMap<String, Any>, dict_anchor: HashMap<String, Any>? = null): CompletableFuture<Boolean>{
        val completableFutureSuccess: CompletableFuture<Boolean> = CompletableFuture()

        try {
            when (dict_node["type"] as Int) {
                0 -> { // GLTF2 Model from Flutter asset folder
                    // Get path to given Flutter asset
                    val loader: FlutterLoader = FlutterInjector.instance().flutterLoader()
                    val key: String = loader.getLookupKeyForAsset(dict_node["uri"] as String)

                    // Add object to scene
                    modelBuilder.makeNodeFromGltf(viewContext, dict_node["name"] as String, key, dict_node["transform"] as ArrayList<Double>)
                            .thenAccept{node ->
                                val anchorName: String? = dict_anchor?.get("name") as? String
                                val anchorType: Int? = dict_anchor?.get("type") as? Int
                                if (anchorName != null && anchorType != null) {
                                    val anchorNode = arSceneView.scene.findByName(anchorName) as AnchorNode?
                                    if (anchorNode != null) {
                                        anchorNode.addChild(node)
                                    } else {
                                        completableFutureSuccess.complete(false)
                                    }
                                } else {
                                    arSceneView.scene.addChild(node)
                                }
                                completableFutureSuccess.complete(true)
                            }
                            .exceptionally { throwable ->
                                // Pass error to session manager (this has to be done on the main thread if this activity)
                                val mainHandler = Handler(viewContext.mainLooper)
                                val runnable = Runnable {sessionManagerChannel.invokeMethod("onError", listOf("Unable to load renderable" +  dict_node["uri"] as String)) }
                                mainHandler.post(runnable)
                                completableFutureSuccess.completeExceptionally(throwable)
                                null // return null because java expects void return (in java, void has no instance, whereas in Kotlin, this closure returns a Unit which has one instance)
                            }
                }
                1 -> { // GLB Model from the web
                    modelBuilder.makeNodeFromGlb(viewContext, dict_node["name"] as String, dict_node["uri"] as String, dict_node["transform"] as ArrayList<Double>)
                            .thenAccept{node ->
                                val anchorName: String? = dict_anchor?.get("name") as? String
                                val anchorType: Int? = dict_anchor?.get("type") as? Int
                                if (anchorName != null && anchorType != null) {
                                    val anchorNode = arSceneView.scene.findByName(anchorName) as AnchorNode?
                                    if (anchorNode != null) {
                                        anchorNode.addChild(node)
                                    } else {
                                        completableFutureSuccess.complete(false)
                                    }
                                } else {
                                    arSceneView.scene.addChild(node)
                                }
                                completableFutureSuccess.complete(true)
                            }
                            .exceptionally { throwable ->
                                // Pass error to session manager (this has to be done on the main thread if this activity)
                                val mainHandler = Handler(viewContext.mainLooper)
                                val runnable = Runnable {sessionManagerChannel.invokeMethod("onError", listOf("Unable to load renderable" +  dict_node["uri"] as String)) }
                                mainHandler.post(runnable)
                                completableFutureSuccess.completeExceptionally(throwable)
                                null // return null because java expects void return (in java, void has no instance, whereas in Kotlin, this closure returns a Unit which has one instance)
                            }
                }
                else -> {
                    completableFutureSuccess.complete(false)
                }
            }
        } catch (e: java.lang.Exception) {
            completableFutureSuccess.completeExceptionally(e)
        }

        return completableFutureSuccess
    }

    private fun transformNode(name: String, transform: ArrayList<Double>) {
        val node = arSceneView.scene.findByName(name)
        node?.let {
            val transformTriple = deserializeMatrix4(transform)
            it.worldScale = transformTriple.first
            it.worldPosition = transformTriple.second
            it.worldRotation = transformTriple.third
        }
    }

    private fun onTap(hitTestResult: HitTestResult, motionEvent: MotionEvent?): Boolean {
        val frame = arSceneView.arFrame
        if (hitTestResult.node != null && motionEvent?.action == MotionEvent.ACTION_DOWN) {
            objectManagerChannel.invokeMethod("onNodeTap", listOf(hitTestResult.node?.name))
            return true
        }
        if (motionEvent != null && motionEvent.action == MotionEvent.ACTION_DOWN) {
            val allHitResults = frame?.hitTest(motionEvent) ?: listOf<HitResult>()
            val planeAndPointHitResults = allHitResults.filter { ((it.trackable is Plane) || (it.trackable is Point))  }
            val serializedPlaneAndPointHitResults: ArrayList<HashMap<String, Any>> = ArrayList(planeAndPointHitResults.map { serializeHitResult(it) })
            sessionManagerChannel.invokeMethod("onPlaneOrPointTap", serializedPlaneAndPointHitResults)
            return true
        }
        return false
    }

    private fun addPlaneAnchor(transform: ArrayList<Double>, name: String): Boolean {
        return try {
            val position = floatArrayOf(deserializeMatrix4(transform).second.x, deserializeMatrix4(transform).second.y, deserializeMatrix4(transform).second.z)
            val rotation = floatArrayOf(deserializeMatrix4(transform).third.x, deserializeMatrix4(transform).third.y, deserializeMatrix4(transform).third.z, deserializeMatrix4(transform).third.w)
            val anchor: Anchor = arSceneView.session!!.createAnchor(Pose(position, rotation))
            val anchorNode = AnchorNode(anchor)
            anchorNode.name = name
            anchorNode.setParent(arSceneView.scene)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun removeAnchor(name: String) {
            val anchorNode = arSceneView.scene.findByName(name) as AnchorNode?
            anchorNode?.let{
                // Remove corresponding anchor from tracking
                anchorNode.anchor?.detach()
                // Remove children
                for (node in anchorNode.children) {
                    node.setParent(null)
                }
                // Remove anchor node
                anchorNode.setParent(null)
            }
    }

}
