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

    lib = pkgs.lib;

    buildToolsVersion = "35.0.0";
    sdkVersion = "35";
    androidSdk = let
      platformToolsVersion = "35.0.1";

      androidComposition = pkgs.androidenv.composeAndroidPackages {
        includeNDK = false;
        includeSystemImages = false;
        includeEmulator = false;
        platformVersions = [sdkVersion];
        buildToolsVersions = [buildToolsVersion];
        platformToolsVersion = platformToolsVersion;
      };
    in
      androidComposition.androidsdk;

    pname = "jomc";
  in {
    devShells.${system}.default = pkgs.mkShell rec {
      packages = with pkgs; [
        android-studio
        android-tools
        gradle
        jdt-language-server

        (let
          adb = lib.getExe' pkgs.android-tools "adb";
        in
          pkgs.writers.writeBashBin "prun" ''
            dev="192.168.1.2:40063"
            ${adb} -s $dev install app/build/outputs/apk/debug/app-debug.apk
            ${adb} -s $dev shell am start -n com.${pname}/.MainActivity
          '')
      ];

      ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";
      GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${ANDROID_SDK_ROOT}/build-tools/${buildToolsVersion}/aapt2";
    };
  };
}
