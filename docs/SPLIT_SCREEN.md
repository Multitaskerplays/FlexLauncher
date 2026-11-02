# Split screen

FlexLauncher can show any two apps side by side, at a divider you choose, as real Android
windows — no root, no vendor launcher, no freeform hack beyond one global setting.

```
long-press an app  →  Split screen  →  pick the other app  →  drag the divider  →  Start split
```

The picker remembers the last pair and the divider, so re-running a favourite pair is
`long-press an app → Split screen → Start split`, or `Split screen (repeat last split)` from
any app's long-press menu.

## Why freeform, and not the box's "split screen"?

Car boxes (Carlinkit, and the `suding`/`speedplay` family generally) ship a split feature
that is only a stub:

* `com.sd.factoryconfig/.SplitAppActivity` pins a shortcut carrying two package names and a
  ratio;
* tapping it broadcasts `com.android.intent.ext.main.split.two.screen`;
* the **only** receiver is `com.android.launcher3`'s home activity
  (`QuickstepLauncher → SplitScreenManager.launchExistingSplitPair`).

Registering that receiver is a side effect of launcher3 *being the home screen*. Once
FlexLauncher is HOME, launcher3 never starts, the receiver never exists, and the broadcast
is dropped without an error. There is no public API to invoke the vendor's split manager
from another app. So a third-party launcher has to place the windows itself.

Freeform windows do exactly that. `windowingMode=5` plus an explicit `launchBounds` rect
gives pixel-exact placement, which is what a car's fixed projection area needs — the
platform split has no say in the divider position.

## The one requirement

`enable_freeform_support` must be on while the split starts. FlexLauncher flips it itself,
either:

* directly, if it holds `WRITE_SECURE_SETTINGS`:

  ```sh
  adb shell pm grant com.flexlauncher android.permission.WRITE_SECURE_SETTINGS
  ```

* or through Shizuku's shell (`settings put global enable_freeform_support 1`), if Shizuku
  is running and FlexLauncher has its permission.

If neither is available the picker says so instead of failing silently. The flag is
switched back off a few seconds after launch, because SystemUI draws window title bars when
it *starts* with freeform enabled; the already-created windows stay put.

## Geometry

`launchBounds` is in display pixels of the app area, **not** the panel. On the tested box:

| | |
|---|---|
| Panel | 2374 px (the vendor status strip is excluded from apps) |
| App area | 2270×1016 |
| Divider at **63%** of the panel | 1495 px — the edge of the car's CarPlay card |
| Bottom inset | 45 px, so windows stop above the car's dock |
| Overscan | 10 px, so the car does not show a wallpaper border |

The percentage in the picker is a share of the **panel**, the same basis as the vendor's
`persist.splitscreen.percent` (63:37 of 2374 px = 1495 px). That is deliberate: it is the
number people already know from the stock split, and it does not change when a box reports a
different app area. The pixels are then clamped into the app area, so the panes still line up
with the 2270 px surface the windows actually live on.

Verified on the box: left pane `(-10, 0, 1495, 971)`, right pane `(1495, 0, 2280, 971)`; the
window manager shifts both down by the 36 px status bar and they abut exactly at 1495.

`currentWindowMetrics.bounds` reports the full 2374 px panel (it includes the vendor's 104 px
strip), while `DisplayMetrics` reports the 2270 px app area — using the wrong one here moves
the divider to 1566 px and leaves a 70 px wallpaper gap between the panes.

## Checking it worked

```sh
adb shell dumpsys activity activities | grep -E "windowingMode|rootTaskId"
```

Both apps should be in tasks with `mode=freeform`, and `adb shell dumpsys window | grep -i
freeform` lists the two frames at the bounds above.

## Limitations

* Freeform windows are ordinary tasks: going home hides them, recents shows them, and each
  app can only be in one window at a time.
* No persistence across reboots or launcher restarts — re-launching is one long-press.
* `launchBounds` only takes effect when a task is created. Re-running a split over a pane
  that is already open leaves that window where it is, so changing the divider needs the app
  closed first (the picker shows a hint). Verified with and without `CLEAR_TASK`.
* Apps that refuse resizing (`resizeableActivity=false`) may letterbox inside their pane.
* If SystemUI is restarted while the flag is still on, windows grow title bars; the flag is
  restored after each launch for that reason.
* Tested on one device (Carlinkit Ultra AI Box, Android 15, 2374×1016). Other boxes and
  tablets should work with adjusted inset/overscan, but are untested.

## Code map

| File | Role |
|---|---|
| `split/SplitLauncher.kt` | enables freeform, launches both panes, remembers the pair |
| `split/SplitGeometry.kt` | pure pixel maths (`bounds`) + activity metrics |
| `split/SplitPickerActivity.kt` | the picker: two slots, divider, car insets |
| `split/SplitPrefs.kt` | last pair / ratio / inset / overscan in SharedPreferences |
| `split/SplitGeometryCalculatorTest.kt` | asserts the car geometry numbers |
