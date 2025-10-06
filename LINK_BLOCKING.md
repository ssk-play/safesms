# Link Blocking Feature

SafeSms is designed with child safety in mind. The primary safety feature is **link blocking** - preventing all URLs, phone numbers, and addresses from being clickable in messages.

## How It Works

### Jetpack Compose Text
SafeSms uses Jetpack Compose's `Text` composable, which **does NOT automatically detect or activate links** by default. Unlike Android's traditional `TextView` with `autoLink` enabled, Compose Text requires explicit `AnnotatedString` with URL annotations to make links clickable.

### Implementation

#### Message Body (ConversationScreen.kt)
```kotlin
// SAFETY FEATURE: Links are NOT clickable
// Compose Text does not automatically detect or activate links
// This keeps children safe from accidental web browsing
Text(
    text = message.body,
    modifier = Modifier.padding(12.dp),
    // No linkAnnotation, no clickable modifier
)
```

#### Message Preview (SmsListScreen.kt)
```kotlin
// SAFETY FEATURE: Links in message preview are NOT clickable
Text(
    text = thread.snippet,
    style = MaterialTheme.typography.bodyMedium,
    // No linkAnnotation, no clickable modifier
)
```

## What's Blocked

✅ **All URLs are plain text only**
- http://example.com → Not clickable
- https://website.com → Not clickable
- www.site.com → Not clickable

✅ **Phone numbers are plain text only**
- +1-234-567-8900 → Not clickable
- (123) 456-7890 → Not clickable

✅ **Email addresses are plain text only**
- email@example.com → Not clickable

✅ **Physical addresses are plain text only**
- 123 Main St → Not clickable

## Technical Details

### What We DON'T Use
❌ `AnnotatedString.Builder().pushUrlAnnotation()` - Would make links clickable
❌ `LinkAnnotation` - Would make links clickable
❌ `ClickableText` - Would make text clickable
❌ `TextView` with `autoLink` - Would auto-detect and activate links
❌ `BasicTextField` with link detection - Would activate links

### What We DO Use
✅ Simple `Text(text = string)` - Shows text only, no interactions
✅ Pure string display without annotations
✅ No click handlers on text content

## Verification

To verify link blocking is working:

1. Send a message containing URLs (e.g., "Check out https://example.com")
2. Open the message in SafeSms
3. Try to tap on the URL
4. **Expected result**: Nothing happens, URL is not clickable
5. **If clickable**: Link blocking is NOT working (bug)

## Notifications

Notifications use `NotificationCompat.MessagingStyle` which may auto-detect links in the system notification UI. This is controlled by Android's system UI and cannot be disabled at the app level. However:
- Links in notifications are less risky (smaller tap targets)
- Opening the app from notification shows non-clickable messages
- Quick Reply maintains the same link-blocking safety

## Future Considerations

### Option 1: Keep Current (Recommended)
- Simplest implementation
- Relies on Compose's default behavior
- No link detection overhead
- Clear code with safety comments

### Option 2: Visual Masking
- Detect URLs using regex
- Replace with "[Link removed for safety]"
- More explicit but requires processing

### Option 3: Pattern Highlighting (Without Clicks)
- Detect URLs using regex
- Style them differently (e.g., underline)
- But keep them non-clickable
- Visual indicator without risk

## Maintenance Notes

⚠️ **IMPORTANT**: When updating the UI, ensure:
1. Do NOT add `AnnotatedString` with URL annotations
2. Do NOT use `ClickableText` for message content
3. Do NOT enable `autoLink` if switching to `AndroidView`
4. Do NOT add click handlers to message text
5. Always test with messages containing URLs

## Testing Checklist

- [ ] Send message with http:// URL
- [ ] Send message with https:// URL
- [ ] Send message with www. URL
- [ ] Send message with phone number
- [ ] Send message with email address
- [ ] Verify none are clickable in message view
- [ ] Verify none are clickable in list preview
- [ ] Test on different Android versions
- [ ] Test with different URL formats

## Related Files

- `app/src/main/kotlin/ssk/safesms/ui/screens/ConversationScreen.kt` - Message display
- `app/src/main/kotlin/ssk/safesms/ui/screens/SmsListScreen.kt` - Message preview
- `app/src/main/kotlin/ssk/safesms/notification/SmsNotificationManager.kt` - Notifications

---

**SafeSms - Safe Messaging for Kids**
Keeping children focused on communication, free from web distractions.
