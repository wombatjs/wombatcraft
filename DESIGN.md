---
name: Wombatcraft
description: A friendly purple Minecraft launcher with a wombat companion.
colors:
  shell: "#171220"
  sidebar: "#21192d"
  sidebar-border: "#392c49"
  surface: "#261e32"
  surface-border: "#392e47"
  text: "#f5efff"
  muted: "#c4b6d8"
  control: "#30243f"
  control-border: "#59436e"
  control-hover: "#45305e"
  hover-border: "#b58aed"
  focus: "#d9baff"
  disabled-text: "#b7a5c8"
  disabled-border: "#4c3c5d"
  nav-active: "#4e2d78"
  nav-active-border: "#8051b0"
  nav-hover: "#352545"
  primary: "#783ee3"
  primary-border: "#af7cff"
  primary-hover: "#8c51ee"
  primary-disabled: "#49325f"
  primary-disabled-text: "#baa5cf"
  primary-disabled-border: "#674d7e"
  progress-track: "#362947"
  progress-fill: "#b27bff"
  input: "#21182d"
  input-border: "#79579b"
  server-text: "#d9c0ff"
  wordmark: "#fff9ff"
  wordmark-outline: "#120c1d"
  tagline: "#fff3ff"
typography:
  display:
    fontFamily: "Silkscreen, Segoe UI"
  brand:
    fontFamily: "Segoe UI"
    fontSize: "24px"
    fontWeight: 700
  heading:
    fontFamily: "Segoe UI"
    fontSize: "22px"
    fontWeight: 600
  action:
    fontFamily: "Segoe UI"
    fontSize: "25px"
    fontWeight: 700
  body:
    fontFamily: "Segoe UI"
    fontSize: "15px"
  muted:
    fontFamily: "Segoe UI"
    fontSize: "13px"
  tagline:
    fontFamily: "Segoe UI"
    fontSize: "17px"
rounded:
  surface: "12px"
  button: "8px"
  input: "6px"
  progress: "2px"
spacing:
  sidebar-gap: "9px"
  surface-gap: "12px"
  main-gap: "14px"
  pack-gap: "18px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.text}"
    typography: "{typography.action}"
    rounded: "{rounded.button}"
    padding: "17px 24px"
  button-primary-hover:
    backgroundColor: "{colors.primary-hover}"
  button-primary-disabled:
    backgroundColor: "{colors.primary-disabled}"
    textColor: "{colors.primary-disabled-text}"
  button-secondary:
    backgroundColor: "{colors.control}"
    textColor: "{colors.text}"
    typography: "{typography.body}"
    rounded: "{rounded.button}"
    padding: "11px 16px"
  button-secondary-hover:
    backgroundColor: "{colors.control-hover}"
  button-secondary-disabled:
    backgroundColor: "{colors.control}"
    textColor: "{colors.disabled-text}"
  navigation:
    backgroundColor: "transparent"
    textColor: "{colors.text}"
    rounded: "{rounded.button}"
    padding: "14px 16px"
  navigation-selected:
    backgroundColor: "{colors.nav-active}"
  surface:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.surface}"
    padding: "20px 22px"
  memory-input:
    backgroundColor: "{colors.input}"
    textColor: "{colors.text}"
    rounded: "{rounded.input}"
    padding: "9px"
  progress:
    backgroundColor: "{colors.progress-track}"
    rounded: "{rounded.progress}"
    height: "5px"
---

# Design System: Wombatcraft

## Overview

**Creative North Star: "Play. Mod. Wombat."**

A friendly brown wombat welcomes players into a Minecraft voxel sunset. Dark plum surfaces and saturated purple actions follow the user's replacement launcher reference, with block lettering and clear sidebar navigation.

The native Windows launcher organizes one official pack into approachable pages. Illustrated warmth belongs to the banner and mascot; controls remain readable, steady and practical. This purple launcher supersedes the previous woodland/copper launcher design. The woodland direction remains scoped to the separate Minecraft menu.

**Key Characteristics:**
- Friendly wombat mascot and voxel sunset banner.
- Dark plum shell, layered panels and purple primary actions.
- Block wordmark with readable Segoe UI controls.
- Persistent operation status and distinct account/settings pages.

Extracted from launcher/interface.py, launcher/main.py and the purple compact review capture. Authentication is unavailable in this preview; Play is disabled accordingly. Pack update testing is separate from UI review and does not verify Microsoft authentication or a complete game launch. The earlier woodland review verdict does not apply automatically to this replacement.

## Colors

Purple is an intentional user-selected identity here. Frontmatter is the normative primitive source. Sidecar tonal ramps are synthesized preview aids, not additional runtime colors.

### Primary
- Primary violet makes Play dominant; primary-hover provides pointer feedback. The disabled primary has its own muted fill, text and border, visibly distinct from enabled Play.
- Progress-fill carries installation activity. Selected navigation uses a quieter violet with a defined border.

### Neutral
- Shell, sidebar and surface form increasingly light plum layers. Surface and sidebar borders separate regions without shadows.
- Text is warm near-white. Muted text handles labels and persistent status; server-text remains pale lavender in checking, online and unavailable states.
- Control, hover, disabled and focus colors define supporting actions. Banner wordmark and tagline have their own bright text tokens and the wordmark a dark outline.

**The Verified Status Rule.** Show online wording and player counts only after a successful server status response.

## Typography

Silkscreen supplies the pixel-like banner wordmark, falling back to Segoe UI if its bundled font cannot load. Its native pixel size is max(26, min(49, int(banner width × 0.048))); it is drawn at x=26 and baseline 53% of banner height with a six-pixel dark outline. The tagline sits 35 pixels below, at x=28.

Segoe UI handles the sidebar brand, page and card headings, Play, body and muted labels using the frontmatter roles. Selected navigation uses weight 600. There is no authored line-height, letter spacing or type animation. Body descriptions, status and authentication notes wrap.

## Layout

The window defaults to 1220 by 760 logical pixels with a 1000 by 680 minimum. Compact mode uses that minimum. There are no web breakpoints or mobile reflow rules; the sidebar stays 204 pixels wide and the main area stretches.

The root has zero margins and spacing. Sidebar margins are 16/24/16/20 pixels (left/top/right/bottom), with sidebar-gap spacing. Main margins are 24/22/24/18 pixels and main-gap spacing. Card padding and gaps use the surface tokens. The Home introduction/server split uses 3:2 stretch with a 14-pixel gap. Pack content uses an 18-pixel gap. Status reserves 34 pixels beneath the five-pixel progress strip; the unavailable-authentication note remains below it across pages.

The banner is 186–240 pixels tall. Artwork covers its area, aligns right and centers vertically. The left overlay extends across 78% of banner width. Sidebar mascot fits a 150 by 112 pixel box while retaining aspect ratio. Navigation remains visible as stacked pages switch. Native title-bar appearance belongs to Windows, not the app palette.

## Elevation & Depth

Tonal plum layers, one-pixel borders and the illustrated scene provide depth. No custom shadows or lifted hover effects exist. The banner overlay fades RGB 20/12/32 from alpha 180 to zero. Native Qt supplies indeterminate progress animation; no authored transitions or decorative animation exist. Cursor effects in a capture are not design tokens.

## Shapes

Cards and banner share the surface radius; buttons, input and progress use progressively smaller radii. Cards and controls have one-pixel borders. Standard button focus requests a two-pixel focus border. Object-specific Play and navigation rules can override that generic Qt focus styling; do not claim that every control has an independently verified focus treatment. The wordmark outline is a painted stroke, not a drop shadow.

## Components

### Primary and secondary buttons

Play sits in the Home version/installation row. Its enabled, hover and disabled fills are explicit. Supporting actions use plum controls with lighter hover borders. There is no custom pressed style. Disabled secondary buttons retain their normal fill with muted text and border.

Sign-in requires idle state and a configured Microsoft client ID. Play also requires an available release and refreshes authentication when invoked. Sign-out requires an in-memory account. Background work or a running game disables sign-in, Play, install, memory and sign-out. Navigation, file links, release links and address copying remain available.

### Navigation

Home, Your pack, Account, Settings and About are checkable buttons controlling a native stacked widget. One is selected; selection adds a violet fill, border and semibold weight. Hover uses nav-hover, including on the selected item. The mascot, centered Wombatcraft name and Launcher label identify the sidebar; the tagline and preview version sit at its foot.

### Surfaces and banner

Home has a sunset banner, version/installation/Play row, introduction card and official server card. The introduction opens Your pack. The server card contains returned status, configured address and Copy server address; copying produces a status confirmation. Keep the banner's right-hand wombat visible when cropping. Card headings and wrapped body text share the same hierarchy on the other pages.

### Account and release state

Account begins with an invitation to sign in. Missing Microsoft configuration shows a note directing players to the Modrinth package, both on Account and persistently below the page area. Successful sign-in would show a named welcome and Switch Microsoft account; sign-out resets the account invitation. These implemented branches do not assert working authentication.

Your pack offers installation, local files and releases. The install label is Check for a release until a manifest succeeds, then Install / check for updates. Persistent inline status reports availability, download/install steps, completion and errors. Unpublished-release messaging points to Releases; runtime errors are limited to 500 characters. The screenshot's release state is capture-time evidence, not the current publication status.

### Server and progress states

Server status begins with checking, reports Online and returned counts after success, and Status unavailable on failure. Checks repeat every 30 seconds. Text color is constant across these states. Progress is empty when idle, indeterminate during background work and full after installation; it does not claim a percentage.

### Settings and game lifecycle

The native memory spin box accepts 6–16 GB and defaults to 10 GB; settings.json restores and persists the value. Qt provides arrows and native input interaction; no custom input hover/focus/error style is defined. The settings file action opens launcher files and game log.

After a successful launch, Play would read Minecraft is running; process exit restores the label and reports clean closure or log-based recovery instructions. Native information dialogs prevent closing during work/gameplay and report an already-open instance. These code paths remain distinct from verified end-to-end gameplay.

### Separate Minecraft menu

The existing woodland menu identity and Join Wombatcraft-first hierarchy remain separate. Retain native scaling and keyboard navigation there; this replacement documents launcher tokens only and does not establish menu acceptance.

## Do's and Don'ts

### Do:
- **Do** follow the user's purple launcher reference with plum panels, violet Play and a friendly wombat.
- **Do** keep the official pack, real server status and persistent operation status easy to find.
- **Do** preserve the sunset banner and clear five-page sidebar hierarchy.
- **Do** keep the woodland Minecraft menu direction scoped to that separate surface.

### Don't:
- **Don't** restore copper launcher styling or treat purple as prohibited.
- **Don't** invent online counts or hide unavailable authentication behind an enabled Play button.
- **Don't** add decorative statistics or development explanations to player controls.
- **Don't** equate update tests or UI screenshots with verified authentication and game launch.
