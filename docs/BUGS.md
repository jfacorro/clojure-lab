# BUGS

## Document Tabs
- When the same document is opened several times and it has been modified, the tabs opened after the modification don't show the modified mark in the tab title.

## Syntax highlighting (SH) w/ Rainbow Delimiters (RD)
- If a :default is used for SH or there's a style defined for the delimiters, then they start flickering when RD is active.
  - By removing the default new inserted text is colored in whatever style.

## Rainbow Delimiters
- Should not be active for all langs. Therefore adding it via a hook to the editor creation function is not the correct option.
