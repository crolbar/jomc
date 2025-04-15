{
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";

  outputs = inputs: let
    system = "x86_64-linux";

    pkgs = (
      import inputs.nixpkgs {
        inherit system;
        config = {
          android_sdk.accept_license = true;
          allowUnfree = true;
        };
      }
    );

    sdkVersion = "35";
    buildToolsVersion = "35.0.0";
    platformToolsVersion = "35.0.1";

    androidComposition = pkgs.androidenv.composeAndroidPackages {
      includeNDK = false;
      includeSystemImages = false;
      includeEmulator = false;
      platformVersions = [sdkVersion];
      buildToolsVersions = [buildToolsVersion];
      platformToolsVersion = platformToolsVersion;
      extraLicenses = [
        "android-sdk-preview-license"
        "android-googletv-license"
        "android-sdk-arm-dbt-license"
        "google-gdk-license"
        "intel-android-extra-license"
        "intel-android-sysimage-license"
        "mips-android-sysimage-license"
      ];
    };
    androidSdk = androidComposition.androidsdk;
  in {
    devShells.${system}.default = pkgs.mkShell rec {
      packages = with pkgs; [
        android-studio
        android-tools # adb
        gradle
        jdt-language-server
        (pkgs.writers.writeBashBin "prun" ''
          dev="192.168.1.2:42801"
          ./gradlew build
          adb -s $dev install app/build/outputs/apk/debug/app-debug.apk
          adb -s $dev shell am start -n com.jomc/.MainActivity
        '')
      ];

      ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";
      GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${ANDROID_SDK_ROOT}/build-tools/${buildToolsVersion}/aapt2";
    };
  };
}
