
# react-native-nuance-nlu

## Getting started

`$ npm install react-native-nuance-nlu --save`

### Mostly automatic installation

`$ react-native link react-native-nuance-nlu`

### Manual installation

#### Android

1. Open up `android/app/src/main/java/[...]/MainApplication.java`
  - Add `import com.speech.nuance.NuancePackage;` to the imports at the top of the file
  - Add `new NuancePackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-nuance-nlu'
  	project(':react-native-nuance-nlu').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-nuance-nlu/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-nuance-nlu')
  	```
## Set up Nuance developer account

- Whether you want to use ASR (Advanced speech recognition) or NLU (Natural Language Understanding), head over to Nuance to create a free developer account and acquire your API key etc. on their 'mix' platform.
https://developer.nuance.com/public/index.php?task=mix

- ASR is used when you want to the Nuance software to interpret utterances your users 'say' and return text of what they said. Once you sign up for a developer account, there are no other steps involved before getting started!

- NLU is used when you need to 'understand' what your users mean and take actions based on the interpretations. Once you sign up for a developer account - you will need to set up your project - go to https://developer.nuance.com/mix/nlu, to get started. They have a good guide on there to explain the basics, also check out their youtube channel for instructions on setting up your project.

## Setting up permissions

1. Add the following to your AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>

```

2. Ensure the user is prompted to give permission for RECORD_AUDIO and WRITE_EXTERNAL_STORAGE. See https://facebook.github.io/react-native/docs/permissionsandroid

## Usage
```javascript
import RNNuanceNlu from 'react-native-nuance-nlu';

// Create a session using your Nuance details
RNNuanceNlu.CreateSession(APP_ID, SERVER_HOST, SERVER_PORT, APP_KEY);

// listen in to various events using DeviceEventEmitter (first import {DeviceEventEmitter} from 'react-native'
DeviceEventEmitter.addListener('onRecognition', (event) => {
	// recognition string containing what speech was recognized
	console.log(event.recognition);  
});
DeviceEventEmitter.addListener('onStateChange', (event) => {
	// state goes from IDLE -> LISTENING -> PROCESSING -> IDLE
	console.log(event.state);  
});
// then call 'startAsr' with language, detection type, and recognition type to start recording and processing
RNNuanceNlu.StartAsr('eng-USA', "SHORT", "DICTATION");

```

## More info

- Check out Nuance's android documentation for further information https://developer.nuance.com/public/Help/DragonMobileSDKReference_Android/index.html

## Future work

- This library only currently contains basic ASR and NLU implementation, there's lots more to be included, so if you need anything, please ask!
