import Flutter
import UIKit
import Foundation
import ARKit
import Combine

class IosARView: NSObject, FlutterPlatformView, ARSCNViewDelegate {
    let sceneView: ARSCNView
    let sessionManagerChannel: FlutterMethodChannel
    let objectManagerChannel: FlutterMethodChannel
    var showPlanes = false
    var customPlaneTexturePath: String? = nil
    private var trackedPlanes = [UUID: (SCNNode, SCNNode)]()
    let modelBuilder = ArModelBuilder()
    
    var cancellableCollection = Set<AnyCancellable>() //Used to store all cancellables in (needed for working with Futures)

    init(
        frame: CGRect,
        viewIdentifier viewId: Int64,
        arguments args: Any?,
        binaryMessenger messenger: FlutterBinaryMessenger
    ) {
        self.sceneView = ARSCNView(frame: frame)
        self.sessionManagerChannel = FlutterMethodChannel(name: "arsession_\(viewId)", binaryMessenger: messenger)
        self.objectManagerChannel = FlutterMethodChannel(name: "arobjects_\(viewId)", binaryMessenger: messenger)
        super.init()

        let configuration = ARWorldTrackingConfiguration() // Create default configuration before initializeARView is called
        self.sceneView.delegate = self
        self.sceneView.session.run(configuration)

        self.sessionManagerChannel.setMethodCallHandler(self.onSessionMethodCalled)
        self.objectManagerChannel.setMethodCallHandler(self.onObjectMethodCalled)
    }

    func view() -> UIView {
        return self.sceneView
    }

    func onSessionMethodCalled(_ call :FlutterMethodCall, _ result:FlutterResult) {
        let arguments = call.arguments as? Dictionary<String, Any>
          
        switch call.method {
            case "init":
                //self.sessionManagerChannel.invokeMethod("onError", arguments: ["SessionTEST from iOS"])
                //result(nil)
                initializeARView(arguments: arguments!, result: result)
                break
            default:
                result(FlutterMethodNotImplemented)
                break
        }
    }

    func onObjectMethodCalled(_ call :FlutterMethodCall, _ result:FlutterResult) {
        let arguments = call.arguments as? Dictionary<String, Any>
          
        switch call.method {
            case "init":
                self.objectManagerChannel.invokeMethod("onError", arguments: ["ObjectTEST from iOS"])
                result(nil)
                break
            case "addObjectAtOrigin":
                if let objectPath = arguments!["objectPath"] as? String, let scale = arguments!["scale"] as? Double {
                    addObjectAtOrigin(objectPath: objectPath, scale: Float(scale))
                }
                result(nil)
            case "addWebObjectAtOrigin":
                if let objectURL = arguments!["objectURL"] as? String, let scale = arguments!["scale"] as? Double {
                    addWebObjectAtOrigin(objectURL: objectURL, scale: Float(scale))
                }
                result(nil)            
            default:
                result(FlutterMethodNotImplemented)
                break
        }
    }

    func initializeARView(arguments: Dictionary<String,Any>, result: FlutterResult){
        // Set plane detection configuration
        let configuration = ARWorldTrackingConfiguration()
        if let planeDetectionConfig = arguments["planeDetectionConfig"] as? Int {
            switch planeDetectionConfig {
                case 1: 
                    configuration.planeDetection = .horizontal
                
                case 2: 
                    if #available(iOS 11.3, *) {
                        configuration.planeDetection = .vertical
                    }
                case 3: 
                    if #available(iOS 11.3, *) {
                        configuration.planeDetection = [.horizontal, .vertical]
                    }
                default: 
                    configuration.planeDetection = []
            }
        }

        // Set plane rendering options
        if let configShowPlanes = arguments["showPlanes"] as? Bool {
            showPlanes = configShowPlanes
            if (showPlanes){
                // Visualize currently tracked planes
                for plane in trackedPlanes.values {
                    plane.0.addChildNode(plane.1)
                }
            } else {
                // Remove currently visualized planes
                for plane in trackedPlanes.values {
                    plane.1.removeFromParentNode()
                }
            }
        }
        if let configCustomPlaneTexturePath = arguments["customPlaneTexturePath"] as? String {
            customPlaneTexturePath = configCustomPlaneTexturePath
        }

        // Set debug options
        var debugOptions = ARSCNDebugOptions().rawValue
        if let showFeaturePoints = arguments["showFeaturePoints"] as? Bool {
            if (showFeaturePoints) {
                debugOptions |= ARSCNDebugOptions.showFeaturePoints.rawValue
            }
        }
        if let showWorldOrigin = arguments["showWorldOrigin"] as? Bool {
            if (showWorldOrigin) {
                debugOptions |= ARSCNDebugOptions.showWorldOrigin.rawValue
            }
        }
        self.sceneView.debugOptions = ARSCNDebugOptions(rawValue: debugOptions)
    
        // Update session configuration
        self.sceneView.session.run(configuration)
    }

    func renderer(_ renderer: SCNSceneRenderer, didAdd node: SCNNode, for anchor: ARAnchor) {
        
        if let planeAnchor = anchor as? ARPlaneAnchor{
            let plane = modelBuilder.makePlane(anchor: planeAnchor, flutterAssetFile: customPlaneTexturePath)
            trackedPlanes[anchor.identifier] = (node, plane)
            if (showPlanes) {
                node.addChildNode(plane)
            }
        }
    }

    func renderer(_ renderer: SCNSceneRenderer, didUpdate node: SCNNode, for anchor: ARAnchor) {
        
        if let planeAnchor = anchor as? ARPlaneAnchor, let plane = trackedPlanes[anchor.identifier] {
            modelBuilder.updatePlaneNode(planeNode: plane.1, anchor: planeAnchor)
        }
    }

    func renderer(_ renderer: SCNSceneRenderer, didRemove node: SCNNode, for anchor: ARAnchor) {
        trackedPlanes.removeValue(forKey: anchor.identifier)
    }

    func addObjectAtOrigin(objectPath: String, scale: Float) {
        // Get path to given Flutter asset
        let key = FlutterDartProject.lookupKey(forAsset: objectPath)

        // Add object to scene
        if let node: SCNNode = modelBuilder.makeNodeFromGltf(modelPath: key, worldScale: SCNVector3Make(scale, scale, scale), worldPosition: SCNVector3Make(0,0,0), worldRotation: SCNQuaternion(1,0,0,0)) {
            sceneView.scene.rootNode.addChildNode(node)
            
        } else {
            self.sessionManagerChannel.invokeMethod("onError", arguments: ["Unable to load renderable \(objectPath)"])
        }
    }
    
    func addWebObjectAtOrigin(objectURL: String, scale: Float) {

        // Add object to scene

        modelBuilder.makeNodeFromWebGlb(modelURL: objectURL, worldScale: SCNVector3Make(scale, scale, scale), worldPosition: SCNVector3Make(0,0,0), worldRotation: SCNQuaternion(1,0,0,0))
        .sink(receiveCompletion: {
                        completion in print("Async Model Downloading Task completed: ", completion)
        }, receiveValue: { val in
            if let node: SCNNode = val {
                self.sceneView.scene.rootNode.addChildNode(node)
            } else {
                self.sessionManagerChannel.invokeMethod("onError", arguments: ["Unable to load renderable \(objectURL)"])
            }
        }).store(in: &self.cancellableCollection)
        


    }
}
