# Ghost Detector: Spirit Box — Product Contract

Field unit GD-7. This document is the binding definition of what the MVP is, what it
claims, and what it is not allowed to claim. Code, copy and design changes are checked
against it.

## 1. Truth architecture

The product is an instrument for atmosphere and shared attention, not a paranormal
detector. Every surface must be truthful about where its data comes from.

| Surface | What it actually is | What it must never imply |
| --- | --- | --- |
| EMF | Real device magnetometer, field magnitude in µT to one decimal | Not a ghost meter, no invented values when the sensor is missing |
| Signal model (Radar) | Visualization of session activity over time | No distance, no range, no bearing, no metres, no direction |
| Spirit Box | On-device weighted word engine drawing from bundled pools | No microphone, no radio, no FM sweep, no frequency, no Hz, no received transmission |
| Record | Timestamped log of what happened during the session | No verdicts, no scoring of "paranormal activity" |

Fixed labels: the session logs, so the running indicator reads `LOGGING`, never `REC`.
The radar window is labelled `SIGNAL MODEL`. Radar events are only `SWEEP`, `TRACE`,
`CONTACT`, `LOST`.

Disclosure copy shown on first launch and in every shared record:
`REAL DATA · EXPERIMENTAL INTERPRETATION`.

## 2. MVP boundaries

In scope for the MVP:

- One investigation session at a time, with three views of that same session:
  Radar (signal model), Box (word engine), EMF (magnetometer).
- Baseline measured at session start; threshold derived from that baseline.
- Question logging with prepared and custom questions.
- Marking a moment during a session.
- A saved evidence record per finished session, readable and shareable as text.
- A local archive of records on the device.
- First-launch briefing that states the truth architecture.

Explicitly out of scope for the MVP:

- Microphone capture, audio recording or playback of any kind.
- Photo, video or camera features.
- Location, notifications, contacts, Bluetooth.
- Accounts, cloud sync, backend services, analytics, advertising.
- Purchases and subscriptions (the free-tier limits below exist, but no purchase flow).
- Any second language; English only at launch, all strings still routed through
  localization keys.

## 3. Free tier limits

- Session length: 15:00, after which the session closes itself and the record is written.
- Archive capacity: 3 records, oldest replaced when a fourth is filed.
- Grace period: a session survives 5 minutes in the background; longer ends it.

## 4. Session state machine

`IDLE → CALIBRATING → RUNNING → ENDED → IDLE`

- `IDLE`: nothing moves, nothing reads out, no baseline is displayed.
- `CALIBRATING`: 3.5 s of real samples; the header reads `BASELINE`.
- `RUNNING`: header reads `LOGGING`; mode switching never restarts the session.
- `ENDED`: the record is written, then the app returns to `IDLE`.

Ending a session is deliberate: HOLD STOP only. There is no close control and no tab bar
during a session; Android Back asks for confirmation.

## 5. Visual contract

Locked direction: late-1970s field instrument. Matte anthracite enclosure, amber phosphor
displays, bone silkscreen text, dim amber for inactive segments and history.

Red (`signal`) is reserved for exactly four things: warnings, threshold states, hold
progress, and destructive confirmation. Nothing else may be red.

Forbidden: neon green, ghost imagery, jump scares, white or light frames anywhere
including startup, flashing, strobe, shake, glitch, VHS effects, heavy glow, infinite
layout animation, and any fill lighter than `caseEdge` larger than 40×40dp.

## 6. Accessibility and comfort

- Minimum touch target 56×56dp; primary controls sit within lower thumb reach.
- Text must stay readable at Android font scale 1.3.
- Reduced Motion disables every non-essential movement (sweep, pulse, transitions);
  hold-to-stop progress remains because it is required feedback for a destructive action.
- Decorative inactive segments and graticules are hidden from accessibility services.

## 7. Data and privacy

Everything stays on the device. Records are stored as JSON in app-private storage.
No network calls are made by the product. Sharing is an explicit user action through the
system share sheet.
