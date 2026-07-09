# Papra Android (Drive-styled client scaffold)

A starting Android app for your Papra fork, visually modeled on Google Drive
(Material 3, blue brand color, grid/list toggle, FAB upload, color-coded
file-type icons, org switcher standing in for Drive's account switcher).

## What's included

- **Kotlin + Jetpack Compose + Material 3**, dynamic color on Android 12+
- **Dual auth**, chosen at login: Papra **API key** or **email/password**
  (Better Auth) session cookie — both point at a user-entered self-hosted
  server URL, since Papra isn't a fixed single-tenant service
- **Retrofit client** covering: organizations, documents (list/get/upload/
  rename/trash), document file download, tags, document statistics
- **Home screen**: grid/list toggle, org switcher, FAB with "Upload file" /
  "Scan document" menu
- **Document detail screen**: icon preview, tags as chips, notes, delete
- **Search screen**: full-text query reusing Papra's `searchQuery` param

## What's stubbed / needs finishing

- **File picker + actual upload wiring** — `onUploadFile` in `NavGraph.kt`
  is a TODO; wire it to `ActivityResultContracts.OpenDocument()` (or
  `GetContent()`), copy the picked `Uri` to a temp `File`, then call
  `documentRepository.uploadDocument(orgId, file, mimeType)`.
- **Camera scan flow** — `onScanDocument` TODO; simplest path is
  CameraX `ImageCapture` or just `MediaStore` capture intent, then treat the
  resulting JPEG like an upload.
- **Document preview** — detail screen shows a big file-type icon, not an
  actual PDF/image renderer. For images, load via Coil from the
  `/documents/{id}/file` endpoint (needs an authenticated `Coil ImageLoader`
  using the same `AuthInterceptor`). For PDFs, `PdfRenderer` or a WebView.
- **Better Auth cookie handling** — `AuthRepository.signInWithEmail` reads
  the raw `set-cookie` header and replays it verbatim on every request. This
  works for a first pass but doesn't handle cookie expiry/refresh — check
  what your fork's Better Auth config actually returns and adjust
  `SessionStore`/`AuthInterceptor` accordingly.
- **Context menus** (rename, move, delete, share) on grid/list items are
  `onMoreClick = {}` placeholders — wire to a bottom sheet.
- **Folder browsing / breadcrumbs** — only flat document lists are wired up.
  If your fork's folder hierarchy is active, add a `folderId` param to
  `listDocuments` and a breadcrumb bar like Drive's.
- **Pagination** — `listDocuments` always fetches page 0; add infinite
  scroll using `pageIndex`/`documentsCount`.

## API assumptions

Endpoints in `PapraApiService.kt` mirror the documented Papra REST API
(`/api/organizations`, `/api/organizations/{id}/documents`, etc.). Since
this is *your* fork, diff these paths/fields against your actual server
routes (particularly if you've renamed fields or changed the folder model)
before wiring up the picker/camera flows above.

## Running it

This scaffold has source + Gradle config but no Gradle wrapper jar (binary,
can't be generated here). Open the folder in Android Studio (Koala or
newer) — it will offer to generate the wrapper automatically — or run:

```
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```

Then point the app at your server URL on first launch (e.g.
`https://papra.yourdomain.com` or `http://192.168.1.x:1221` for local dev —
cleartext HTTP is allowed by `network_security_config.xml` for self-hosted
setups without TLS).
