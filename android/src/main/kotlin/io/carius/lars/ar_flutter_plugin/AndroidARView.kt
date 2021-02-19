package io.carius.lars.ar_flutter_plugin

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.RenderableDefinition
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView


//import com.google.ar.core.PointCloud;



import java.nio.FloatBuffer;

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
    // UI variables
    private lateinit var arSceneView: ArSceneView
    private var showFeaturePoints = false;
    private var pointCloudNode = Node();

    //Method channel handlers
    private val onSessionMethodCall = object : MethodChannel.MethodCallHandler {
        override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
            Log.d(TAG, "AndroidARView onsessionmethodcall reveived a call!")
            when (call.method) {
                "init" -> {
                    //sessionManagerChannel.invokeMethod("onError", listOf("SessionTEST from Android"))
                    initializeARView(call, result)
                }
                else -> {
                }
            }
        }
    }
    private val onObjectMethodCall = object : MethodChannel.MethodCallHandler {
        override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
            Log.d(TAG, "AndroidARView onobjectmethodcall reveived a call!")
            when (call.method) {
                "init" -> {
                    objectManagerChannel.invokeMethod("onError", listOf("ObjectTEST from Android"))
                }
                else -> {
                }
            }
        }
    }

    override fun getView(): View {
        return arSceneView
    }

    override fun dispose() {
        // Destroy AR session
        try {
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
        val argShowPlanes: Boolean? = call.argument<Boolean>("showPlanes")
        val argShowWorldOrigin: Boolean? = call.argument<Boolean>("showWorldOrigin")

        arSceneView.scene.addOnUpdateListener { frameTime: FrameTime -> onFrame(frameTime) }

        if (argShowFeaturePoints == true) { //explicit comparison necessary because of nullable type
            arSceneView.scene.addChild(pointCloudNode)
            showFeaturePoints = true
        }
        
        arSceneView.planeRenderer.isVisible = if(argShowPlanes == true) true else false

        result.success(null)
    }

    private fun onFrame(frameTime: FrameTime) {
        //arSceneView.onUpdate(frameTime)

        if (showFeaturePoints){
            // remove points from last frame
            while (pointCloudNode.children?.size ?: 0 > 0) {
                pointCloudNode.children?.first()?.setParent(null)
            }
            // Automatically releases point cloud resources at end of try block.
            var pointCloud = arSceneView.arFrame?.acquirePointCloud()
            // Access point cloud data (returns FloatBufferw with x,y,z coordinates and confidence value).
            val points = pointCloud?.getPoints() ?: FloatBuffer.allocate(0);
            // Check if there are any feature points
            if (points.limit() / 4 >= 1){    
                for (index in 0 until points.limit() / 4){
                    // Create a yellow cube at the given position
                    val singlePoint = Node()                 
                    var cubeRenderable: ModelRenderable? = null      
                    MaterialFactory.makeOpaqueWithColor(viewContext, Color(android.graphics.Color.YELLOW))
                    .thenAccept { material ->
                        val vector3 = Vector3(0.01f, 0.01f, 0.01f)
                        cubeRenderable = ShapeFactory.makeCube(vector3, Vector3(points.get(4 * index), points.get(4 * index + 1), points.get(4 * index +2 )), material)


                        cubeRenderable!!.isShadowCaster = false
                        cubeRenderable!!.isShadowReceiver = false
                    }
                    singlePoint.renderable = cubeRenderable
                    singlePoint.setParent(pointCloudNode)
                }
            }  
            // Release resources
            pointCloud?.release()
        }
    }
        

}
