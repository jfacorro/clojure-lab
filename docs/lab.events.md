Events
======

The following events are supported for any type of component:

**Mouse**

- [Click (:on-click)][1]
- [Pressed (:on-mouse-press)][2]
- Release (:on-mouse-release)
- Entered (:on-mouse-enter)
- Exits (:on-mouse-exit)
- Moved (:on-mouse-moved)
- Dragged (:on-mouse-dragged)
- Wheel Moved (:on-mouse-wheel)

**Keyboard**

- Key Press (:on-key-press)
- Key Release (:on-key-release)

**Component**

- Focus (:on-focus)
- Focus lost (:on-focus-lost)

##Mouse

###Click (:on-click)[](#mouse-click)

An event click handler must be a variadic function which will receive an event arg and any number of extra arguments, depending on the implementation.

For example a button with an click handler would look like this:

    (button :on-click (fn [e & xs] (println e)))

###Pressed (:on-mouse-press)[](#mouse-press)



[1]: #mouse-click
[2]: #mouse-press