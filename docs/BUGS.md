# BUGS

## App
- Initializing two independent apps, causes the event-handling mechanism to replace the previous app atom with the most recent one. This is not aligned with the spirit of the design. Maybe a dynamic var can work in this scenario, though it might get confusing.

## Menu
- Some shortcuts don't work: ctrl alt r, ctrl tab (unless the focus is on an editor).

## Rainbow delimiters
- Implementation too slow.
  - usage lang/offset and lang/search functions are too costly.
