Faurecia-BioFit-Demo-Android
============================

This is an Android 4.4 app that demonstrates how to talk to Faurecia BioFit hardware.

The code contains a BioFit Bluetooth service to abstract how Android handles Bluetooth, but it's not intended to be a stable API. You might find you need to modify how Bluetooth works on different devices, or you may not want to use a service. That's ok! The stable BioFit interface is the Bluetooth service and characteristic definitions. You won't break compatibility with future releases of BioFit as long as you conform to the Bluetooth spec.

The project can be checked out and directly imported into Android Studio.

You will need a physical Android 4.4 device with Bluetooth 4.0 support. This app won't work in the Android simulator. During development we were only able to test on a MotoX. We'd like to test additional devices. Create an issue for devices you have trouble with and we will try to test on them.
