# Changelog

## 0.4.3

* Updates documentation after publishing to [pub.dev](https://pub.dev)

## 0.4.2

* Updates documentation
* Deletes unnecessary files

## 0.4.1

* Adds External Model Management Example: A firebase database is used to store a list of 3D models (name, preview image, URI of the file location in a github repo). These models can be scrolled through and selected from in the example and can then be placed into the scene, uploaded through the Google Cloud Anchor API and downloaded on any other device with the app installed. The downloading user does not need to have the model "pre-installed", it is downloaded on the first occasion.

## 0.4.0

* Adds location manager which can be used to query the device's location (including permission and update handling)
* Adds geoflutterfire to support uploading GPS coordinates alongside anchors and downloading anchors and objects by their location
* Modifies cloud anchor example: Download button now queries and downloads all anchors along with the corresponding objects within a 100m radius of the device's current location
* Bugfix: fixes bug on Android causing some examples to crash because the cloud anchor manager wasn't initialized

## 0.3.0

* BREAKING CHANGE: Converts plugin to adhere to Flutter null safety
* Adds Cloud Anchor functionality (uploading and downloading anchors to/from the Google Cloud Anchor API), including keyless authentication
* Adds Cloud Anchor example demonstrating how to use Firebase to manage cloud anchor IDs and corresponding data (e.g. on-tap texts)
* Adds ```data``` member variable to ```ARNode``` as a flexible variable to hold any information associated with the node

## 0.2.1

* Bugfix: Handles singularities in affine transformation matrix deserialization on Android

## 0.2.0

* Adds AR Anchor as a common representation of anchors on all platforms
* Implements AR Plane Anchor as subclass of AR Anchor: Plane Anchors can be created in Flutter, registered on the target platform and then be used to attach nodes to
* Adds AR Hittest Result as a common representation of hittest results on all platforms. If the setting is activated in the session manager, taps on the platforms are registered and hit test results can be used to trigger callbacks in Flutter (example: hit result world coordinate transforms can be used to place anchors or nodes into the scene)
* Adds option to trigger callbacks when nodes are tapped
* Adds example to showcase hittests, creating and placing anchors and attaching nodes to anchors

## 0.1.0

* Adds AR Node as a common representation of nodes on all platforms
* Custom objects (GLTF2 models from Flutter's asset folders or GLB models from the Internet) can be attached to nodes and are loaded / downloaded asynchronously during runtime to ensure maximum flexibility
* AR Nodes (with attached objects) can be placed in the scene with respect to the world coordinate system, position, rotation and scale can be set and updated during runtime
* Updates debug option functionality: options can be changed at runtime (see Debug Option example)
* Updates examples to showcase current state of the plugin

## 0.0.1

* Coarse Plugin Architecture layed out
* ARView supports iOS (ARKit) and Android (ARCore) devices
* Camera Permission checks added
* Debug options added on both platforms: Feature Points visualization, detected planes visualization and world origin visualization
* Adds possibility to use own texture for plane visualization on both platforms
