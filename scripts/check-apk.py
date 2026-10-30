import pathlib
import zipfile

project = pathlib.Path(__file__).resolve().parent.parent
apk = project / 'downloads/ferro-e-rimbalzi-0.2.0-debug.apk'

assert apk.exists(), f"APK non trovato in {apk}"

with zipfile.ZipFile(apk) as archive:
    assert archive.testzip() is None, 'APK danneggiato'
    names = archive.namelist()
    assert 'classes.dex' in names, 'Codice Android compilato (classes.dex) mancante'
    
    # Verify no web assets leaked into apk
    for n in names:
        assert not n.startswith('assets/web'), f"Trovato asset web inatteso: {n}"
        assert not n.endswith('.html'), f"File HTML inatteso nell'APK: {n}"
    
    manifest = archive.read('AndroidManifest.xml')
    permission = 'android.permission.INTERNET'
    assert permission.encode() not in manifest and permission.encode('utf-16-le') not in manifest, 'Permesso Internet inatteso: l\'app deve essere 100% offline'

print('APK OK: integrità, codice nativo Java (classes.dex), zero asset web e funzionamento 100% offline.')

