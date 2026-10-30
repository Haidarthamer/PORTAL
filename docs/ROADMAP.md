# PORTAL roadmap

## v0.1 — Local inspection foundation
- [x] Native Compose shell and document picker
- [x] PE header preview (x86, x64, ARM64)
- [x] APK/ZIP inventory and native ABI discovery
- [x] IPA bundle filename inspection
- [ ] CI build verified on GitHub Actions

## v0.2 — Deeper static analysis
- Stream PE headers and section tables beyond the first preview window
- ELF and Mach-O header parsers
- DEX/APK metadata and manifest decoding
- Dependency and ABI compatibility reports
- Safe parsing limits and malformed-input test corpus

## v0.3 — Reverse-engineering workspace
- Symbol/import/export views and disassembly integration
- User-authorized debugging workflows
- Reproducible analysis reports
- No DRM, license-check, or access-control bypass features

## v0.4 — Runtime feasibility prototype
- Evaluate Wine + Box64 in app-private Linux userspace on ARM64 Android
- Validate graphics/audio/input paths on real devices
- Box64 is for Linux x86-64 ELF; Wine supplies Windows API compatibility
- DXVK for supported D3D9/10/11-to-Vulkan paths; VKD3D-Proton is separate for D3D12

## v0.5 — Export experiments
- Only rebuild/package when source or compatible native libraries exist
- Explain signing-key and ABI constraints
- Preserve third-party licenses and notices

Each phase needs device tests and documented limitations before being called complete.
