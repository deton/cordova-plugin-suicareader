cordova plugin to read Suica history data
==========================

```javascript
document.addEventListener('deviceready', function () {
    nfc.addTagDiscoveredListener(onNfc, function () {
            console.log("Listening for non-NDEF tags.");
        }, function onFail(e) {
            console.error(e);
        });
}, false);

function onNfc(nfcEvent) {
    var tag = nfcEvent.tag;
    console.log(JSON.stringify(nfcEvent.tag));

    cordova.plugins.SuicaReader.getHistory(10, function onSuccess(data) {
        console.log(JSON.stringify(data));
    }, function onError(e) {
        console.error(e);
    });
}
```

SEE ALSO
================

- [PhoneGap NFC Plugin](https://github.com/chariotsolutions/phonegap-nfc)
- [Suica PASMO CardReader Android App](https://github.com/dongri/CardReader)
