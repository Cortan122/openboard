#!/bin/cat
# shellcheck disable=all
# this is just a list of commands
# it is not meant to be executed

# installing the sdk
yay -S android-sdk
sudo chmod a+w -R /opt/android-sdk
sudo archlinux-java set java-8-openjdk/jre
/opt/android-sdk/tools/bin/sdkmanager 'ndk;21.3.6528147'

# installing the app
sudo archlinux-java set java-11-openjdk
./gradlew assembleDebug
/opt/android-sdk/platform-tools/adb shell "pm uninstall org.dslul.openboard.inputmethod.latin"
/opt/android-sdk/platform-tools/adb install app/build/outputs/apk/debug/app-debug.apk

# release (much faster)
# (debug build is noticeably laggy)
./gradlew --max-workers=1 assemble
/opt/android-sdk/platform-tools/adb install app/build/outputs/apk/release/HeliBoard_2.3+dev8-release.apk

# trying to extract data from the app
/opt/android-sdk/platform-tools/adb shell
run-as org.dslul.openboard.inputmethod.latin
cd /data/data/org.dslul.openboard.inputmethod.latin/
ls -a
