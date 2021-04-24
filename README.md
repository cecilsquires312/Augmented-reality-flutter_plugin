# ar_flutter_plugin

Flutter Plugin for AR (Augmented Reality) - Supports ARKit for iOS and ARCore for Android devices.

Many thanks to Oleksandr Leuschenko for the [arkit_flutter_plugin](https://github.com/olexale/arkit_flutter_plugin) and to Gian Marco Di Francesco for the [arcore_flutter_plugin](https://github.com/giandifra/arcore_flutter_plugin) which both served as a great basis and starting point for this project.

## Getting Started

This plugin is still a work in progress. Keep posted for updates or contribute by creating a [pull request](https://github.com/CariusLars/ar_flutter_plugin/compare)!

If you want to use the plugin before it's officially released, add the following to your `pubspec.yaml` file:
```yaml
dependencies:
  ar_flutter_plugin:
    git: git://github.com/CariusLars/ar_flutter_plugin.git
```

To try out the plugin, it is best to have a look at one of the following examples implemented in the `Example` app:


| Example Name                | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | Link to Code                                                                                                                                   |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| Debug Options               | Simple AR scene with toggles to visualize the world origin, feature points and tracked planes                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | [Debug Options Code](https://github.com/CariusLars/ar_flutter_plugin/blob/main/example/lib/examples/debugoptionsexample.dart)                  |
| Local & Online Objets       | AR scene with buttons to place GLTF objects from the flutter asset folders or GLB objects from the internet at a given position, rotation and scale. Additional buttons allow to modify scale, position and orientation with regard to the world origin after objects have been placed                                                                                                                                                                                                                                                                                                            | [Local & Online Objects Code](https://github.com/CariusLars/ar_flutter_plugin/blob/main/example/lib/examples/localandwebobjectsexample.dart)   |
| Objects & Anchors on Planes | AR Scene in which tapping on a plane creates an anchor with a 3D model attached to it                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             | [Objects & Anchors on Planes Code](https://github.com/CariusLars/ar_flutter_plugin/blob/main/example/lib/examples/objectsonplanesexample.dart) |
| Cloud Anchors               | AR Scene in which objects can be placed, uploaded and downloaded, thus creating an interactive AR experience that can be shared between multiple devices. Currently, the example allows to upload the last placed object along with its anchor and download all anchors within a radius of 100m along with all the attached objects (independent of which device originally placed the objects). As sharing the objects is done by using the Google Cloud Anchor Service and Firebase, this requires some additional setup, please read [Getting Started with cloud anchors](cloudAnchorSetup.md) | [Cloud Anchors Code](https://github.com/CariusLars/ar_flutter_plugin/blob/main/example/lib/examples/cloudanchorexample.dart)                   |


## Roadmap

The next development steps are:
* GPS-tagging anchors to allow efficient querying of anchors by a device's location.

This is a rough sketch of the architecture the plugin will eventually implement:

![ar_plugin_architecture](./AR_Plugin_Architecture_lowlevel.svg)

The cloud backends shown above are only exemplary, the plugin will allow the user to attach their own backend, an example utilizing Firebase will be added to the /example folder.

## Contributing

Contributions to this plugin are very welcome. To contribute code and discuss ideas, [create a pull request](https://github.com/CariusLars/ar_flutter_plugin/compare) or [open an issue](https://github.com/CariusLars/ar_flutter_plugin/issues/new).
