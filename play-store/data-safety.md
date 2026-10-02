# Data safety answers

Answers for the Play Console **App content > Data safety** form, checked against
the release build on 2026-10-02.

## Evidence

- The release merged manifest requests `POST_NOTIFICATIONS`, `WAKE_LOCK`,
  `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, and `FOREGROUND_SERVICE`. It does
  **not** request `INTERNET`, so neither the app nor any library in it can send data
  off the device. The last four come from WorkManager, which runs the daily reminder
  check on the device. Re-check with
  `aapt2 dump permissions app/build/outputs/apk/release/app-release.apk`
  after any dependency change.
- There are no accounts, ads, analytics, crash reporting, or third-party SDKs that
  transmit data.
- Export, Save PDF, Print, and Share PDF each start only from a user's tap, and the
  file goes to the place or app the user picks in the system picker or share sheet.
- Android auto-backup stays on (`android:allowBackup="true"` with full-backup rules).
  If the user has device backup turned on, Android copies the app's data to the
  user's own Google account. The developer never receives it.

## Form answers

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | Not asked when nothing is collected |
| Do you provide a way for users to request that their data is deleted? | Not asked when nothing is collected |

The form then shows "No data collected" and "No data shared with third parties" on
the store listing. A privacy policy link is still required. Use the hosted copy of
`play-store/privacy-policy.html`.

## Why "No" is the right answer

Play Console Help, "Provide information for Google Play's Data safety section"
(<https://support.google.com/googleplay/android-developer/answer/10787469>), checked
2026-10-02:

- **Collect** means transmitting data from your app off a user's device, including
  through libraries or SDKs in the app. This app cannot transmit anything because it
  has no network permission.
- "User data accessed by your app that is only processed locally on the user's
  device and not sent off device does not need to be disclosed."
- Sharing does not include "transferring user data to a third party based on a
  specific user-initiated action, where the user reasonably expects the data to be
  shared." That covers Export, Save PDF, Print, and Share PDF.
- "Even developers with apps that do not collect any user data must complete this
  form and provide a link to their privacy policy."

## Open point: Android auto-backup

That Help page does not mention Android's backup service. Auto-backup is run by the
operating system, controlled by the user's device settings, and stored in the user's
own Google account. The app does not transmit that data and the developer never
receives it, so under the definition above it is not collection by the app. Google
gives no explicit statement either way, though, so this answer rests on that
reading. The privacy policy discloses the backup either way. If Play's guidance
later says otherwise, the alternative is to turn auto-backup off, which would also
make "nothing leaves the device" literally true.
