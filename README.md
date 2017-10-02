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

```json
[
{"year":2017,"month":8,"day":8,"seqNo":987,"kind":"JR","device":"改札機","action":"運賃支払(改札出場)","remain":3464,"inLine":"山陽本","inStation":"広島","outLine":"山陽本","outStation":"新井口","payment":-200}
]
```

SEE ALSO
================

- [PhoneGap NFC Plugin](https://github.com/chariotsolutions/phonegap-nfc)
- [Suica PASMO CardReader Android App](https://github.com/dongri/CardReader)
