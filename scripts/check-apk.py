import pathlib
import zipfile

project = pathlib.Path(__file__).resolve().parent.parent
apk = project / 'downloads/ferro-e-rimbalzi-0.8.0-debug.apk'

assert apk.exists(), f"APK non trovato in {apk}"

with zipfile.ZipFile(apk) as archive:
    assert archive.testzip() is None, 'APK danneggiato'
    names = archive.namelist()
    assert 'classes.dex' in names, 'Codice nativo compilato (classes.dex) mancante'
    
    # Verifica totale assenza di file web
    for n in names:
        assert not n.startswith('assets/web'), f"Trovato asset web inatteso: {n}"
        assert not n.endswith('.html'), f"File HTML inatteso nell'APK: {n}"
    
    manifest = archive.read('AndroidManifest.xml')
    permission = 'android.permission.INTERNET'
    assert permission.encode() not in manifest and permission.encode('utf-16-le') not in manifest, 'Permesso Internet inatteso: il gioco deve essere 100% offline'

print('APK OK: integrità, architettura nativa Java (classes.dex), zero file web, 100% offline.')

