# Changelog

## [2.4.1](https://github.com/albrtbc/memos-android/compare/v2.4.0...v2.4.1) (2026-02-24)


### Bug Fixes

* rename app from MoeMemos to GS Memos ([6d8e7d4](https://github.com/albrtbc/memos-android/commit/6d8e7d432e26f825d63ceb375dac58c6858388b7))
* replace all Moe Memos references with GS Memos in all locales ([2537211](https://github.com/albrtbc/memos-android/commit/25372110da81f67d1a313e398ead8360d3c55584))
* show server host as fallback when account name is empty in settings ([f3e8f35](https://github.com/albrtbc/memos-android/commit/f3e8f356aca6e0810967bbf32dd7b6eda1bdd366))
* update settings links to GS Memos repo and add privacy policy ([5c8bdf5](https://github.com/albrtbc/memos-android/commit/5c8bdf59f83b15027189f30254b11859296c9f99))

## [2.4.0](https://github.com/albrtbc/memos-android/compare/v2.3.0...v2.4.0) (2026-02-24)


### Features

* add altitude support to location across API and local storage ([2cc97e4](https://github.com/albrtbc/memos-android/commit/2cc97e4540ae852f8b2afda761b19900fef8f53a))
* add Paging 3 pagination to main memo list ([7a80cae](https://github.com/albrtbc/memos-android/commit/7a80cae7c1810a9b530d5a712deb15ce2d883b1a))
* add TikTok and Instagram URL embed cards ([8c38e56](https://github.com/albrtbc/memos-android/commit/8c38e56e8d6a5b531973520d24dc011e809810b3))


### Bug Fixes

* improve memo loading UX — auto-refresh on connect, fade-in animations, smaller pages ([0f51daa](https://github.com/albrtbc/memos-android/commit/0f51daa63f40c0465b2dd152f45669a50db050ff))
* map server zoom field to app zoom instead of using altitude ([f4dd77a](https://github.com/albrtbc/memos-android/commit/f4dd77a297445b93c895c999264b551decb70703))
* replace LazyColumn with Column to prevent memo recycling ([e49e9af](https://github.com/albrtbc/memos-android/commit/e49e9af58e3f4cc96b7cc7088f6893a9094f167f))

## [2.3.0](https://github.com/albrtbc/MoeMemosAndroid/compare/v2.2.0...v2.3.0) (2026-02-24)


### Features

* persist and control map zoom level for locations ([c839736](https://github.com/albrtbc/MoeMemosAndroid/commit/c839736a0d4399da3bc5a350edb2303d96f33b1f))


### Bug Fixes

* display location after memo content in card list ([90f3776](https://github.com/albrtbc/MoeMemosAndroid/commit/90f3776384726195a8dab06b374902ea1c764d33))
* style hashtags with rounded background chips ([337fefc](https://github.com/albrtbc/MoeMemosAndroid/commit/337fefc57cb04c092f5dd01ab5002e4fcdbeb1e1))
* switch map tiles to CartoDB to resolve OSM access blocks ([aee61a4](https://github.com/albrtbc/MoeMemosAndroid/commit/aee61a4c0c008b0f011ca813f89721f139f711e0))
* use OSM tiles with User-Agent and improve tile rendering precision ([beb5f93](https://github.com/albrtbc/MoeMemosAndroid/commit/beb5f9361bc68540d6de603f27714d80ad040099))

## [2.2.0](https://github.com/albrtbc/MoeMemosAndroid/compare/v2.1.1...v2.2.0) (2026-02-24)


### Features

* add location support to memo editor ([64ddffd](https://github.com/albrtbc/MoeMemosAndroid/commit/64ddffd2993862cdbfb0074568ac6a8e86c8984b))

## [2.1.1](https://github.com/albrtbc/MoeMemosAndroid/compare/v2.1.0...v2.1.1) (2026-02-24)


### Bug Fixes

* support v-prefixed tags and fallback to debug APK without signing secrets ([5bae37e](https://github.com/albrtbc/MoeMemosAndroid/commit/5bae37e2d03e5e67bdafeddb47e0a725e7290500))

## [2.1.0](https://github.com/albrtbc/MoeMemosAndroid/compare/v2.0.1...v2.1.0) (2026-02-24)


### Features

* add embedded rendering for YouTube, Twitter/X, and Reddit links ([2698552](https://github.com/albrtbc/MoeMemosAndroid/commit/2698552354009691c946df75a64b1d7cc40fb74c))
* add release-please workflow for automated releases ([b0d9c71](https://github.com/albrtbc/MoeMemosAndroid/commit/b0d9c71b1d2cc153cd60d0e2fe253239e5fd018a))
* refresh button also refreshes tags in sidebar ([efd915d](https://github.com/albrtbc/MoeMemosAndroid/commit/efd915dd5bb6f0d6092462bbcb26cd744e441690))
* search includes attachment filenames ([03e5046](https://github.com/albrtbc/MoeMemosAndroid/commit/03e5046d75357115dd6822e9b9c88579a63bc013))
* show edit and share icons directly in memo detail top bar ([2a9147e](https://github.com/albrtbc/MoeMemosAndroid/commit/2a9147e37a8df61c5ae7132a6dd7beca8a199dac))
* show pin/unpin icon directly in memo card header ([45b6112](https://github.com/albrtbc/MoeMemosAndroid/commit/45b61124c5e6aac2caba78c9276aec68ce5221da))


### Bug Fixes

* add include-v-in-tag to release-please config ([a9b8b36](https://github.com/albrtbc/MoeMemosAndroid/commit/a9b8b365e2836c476082382a67f66d0316fd71c4))
* copy link generates double slash in URL ([35d4afb](https://github.com/albrtbc/MoeMemosAndroid/commit/35d4afbe21523e0459eff96a01b2305b2d32f206))
* improve markdown table rendering ([55f7b5e](https://github.com/albrtbc/MoeMemosAndroid/commit/55f7b5e2f98f34fc8e0adc98d7060c075eefb15e))
