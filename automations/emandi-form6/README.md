# MandiSamiti automation: eMandi Form 6 (6R)

AI skill that fills Form 6 on the UP eMandi trader portal and stops for the captcha and Save. Status: DRAFT (dry runs only).

- `SKILL.md`: the skill (Hindi + English, any AI runtime with a browser tool)
- `emandi_notify.py`: Telegram notifier (ready / saved / error). Settings in the hub `.env`: `EMANDI_TEST_MODE`, `EMANDI_CHANNEL_ID`, `EMANDI_CLAUDE_SAVES`. The eMandi bot token has its own Keychain entry (`vivek-k.emandi-bot-token`), separate from the hub bot.
- Linked into AI runtimes via `.agents/skills/emandi-form6` (symlink to this folder).

No credentials, licence numbers or captchas are stored here.
