# Bluetooth Reader (Huifan-HF7000)

Using the Bluetooth Reader library in-tandem with [`BT01`](https://github.com/interwap/BT01) Compile dependency,
an Android application can perform the following:

- Capture Fingerprints
- Match Fingerprints
- Read NFC Tags & Cards

Usage:

Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:

```gradle

    allprojects {
    		repositories {
    			...
    			maven { url 'https://jitpack.io' }
    		}
    	}

 ```

Step 2. Add the dependency
```gradle
    dependencies {
    	        compile 'com.github.interwap:BluetoothReader:v1.0'
    	}
 ```

Get more information about versions and releases here: [`jitpack`](https://jitpack.io/#interwap/BluetoothReader/v1.0)


License
=======

    Copyright 2017 Ikomi Moses

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.