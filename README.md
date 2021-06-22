# ar_flutter_plugin

Flutter Plugin for (collaborative) Augmented Reality - Supports ARKit for iOS and ARCore for Android devices.

Many thanks to Oleksandr Leuschenko for the [arkit_flutter_plugin](https://github.com/olexale/arkit_flutter_plugin) and to Gian Marco Di Francesco for the [arcore_flutter_plugin](https://github.com/giandifra/arcore_flutter_plugin) which both served as a great basis and starting point for this project.

## Getting Started

### Installing

Add the Flutter package to your project by running:

```bash
flutter pub add ar_flutter_plugin
```

Or manually add this to your `pubspec.yaml` file (and run `flutter pub get`):

```yaml
dependencies:
  ar_flutter_plugin: ^0.4.3
```

### Importing

Add this to your code:

```dart
import 'package:ar_flutter_plugin/ar_flutter_plugin.dart';
```
### Example Applications

To try out the plugin, it is best to have a look at one of the following examples implemented in the `Example` app:


| Example Name                | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | Link to Code                                                                                                                                         |
| --------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| Debug Options               | Simple AR scene with toggles to visualize the world origin, feature points and tracked planes                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | [Debug Options Code](https://github.com/CariusLars/ar_flutter_plugin/blob/main/example/lib/examples/debugoptionsexample.dart)                        |
| Local & Online Objets       | AR scene with buttons to place GLTF objects from the flutter asset folders or GLB objects from the internet at a given position, rotation and scale. Additional buttons allow to modify scale, position and orientation with regard to the world origin after objects have been placed                                                                                                                                                                                                                                                                                                                   | [Local & Online Objects Code](https://github.com/CariusLars/ar_flutter_plugin/blob/main/example/lib/examples/localandwebobjectsexample.dart)         |
| Objects & Anchors on Planes | AR Scene in which tapping on a plane creates an anchor with a 3D model attached to it                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | [Objects & Anchors on Planes Code](https://github.com/CariusLars/ar_flutter_plugin/blob/main/example/lib/examples/objectsonplanesexample.dart)       |
| Cloud Anchors               | AR Scene in which objects can be placed, uploaded and downloaded, thus creating an interactive AR experience that can be shared between multiple devices. Currently, the example allows to upload the last placed object along with its anchor and download all anchors within a radius of 100m along with all the attached objects (independent of which device originally placed the objects). As sharing the objects is done by using the Google Cloud Anchor Service and Firebase, this requires some additional setup, please read [Getting Started with cloud anchors](cloudAnchorSetup.md)        | [Cloud Anchors Code](https://github.com/CariusLars/ar_flutter_plugin/blob/main/example/lib/examples/cloudanchorexample.dart)                         |
| External Object Management  | Similar to the Cloud Anchors example, but contains UI to choose between different models. Rather than being hard-coded, an external database (Firestore) is used to manage the available models. As sharing the objects is done by using the Google Cloud Anchor Service and Firebase, this requires some additional setup, please read [Getting Started with cloud anchors](cloudAnchorSetup.md). Also make sure that in your Firestore database, the collection "models" contains some entries with the fields "name", "image", and "uri", where "uri" points to the raw file of a model in GLB format | [External Model Management Code](https://github.com/CariusLars/ar_flutter_plugin/blob/main/example/lib/examples/externalmodelmanagementexample.dart) |

## Contributing

Contributions to this plugin are very welcome. To contribute code and discuss ideas, [create a pull request](https://github.com/CariusLars/ar_flutter_plugin/compare), [open an issue](https://github.com/CariusLars/ar_flutter_plugin/issues/new), or [start a discussion](https://github.com/CariusLars/ar_flutter_plugin/discussions).

## Plugin Architecture

This is a rough sketch of the architecture the plugin implements:

![ar_plugin_architecture](./AR_Plugin_Architecture_highlevel.svg)
