# A Spiffy place to start

Okay... so it's kinda tough to start a web project in Clojure.
Sure, there's [Pedistal](https://github.com/pedestal/pedestal),
but that's pretty much Clojure wrapped around a 15 year old
architecture. Yuck.

So, what's this year's architecture? I think it's
single page apps talking via web sockets to the server... but
the communications is abstracted as `core.async` messages.

That means the app is as responsive as 90s style
client/server apps rather than being 70s style green
screen apps (yeah, [I'm opinionated](http://blog.goodstuff.im/web-framework-manifesto-republished-from-2006-)
but in general, [I do things about it](http://liftweb.net)).

What do I think the best stack for doing web development looks like?

* [Om](https://github.com/swannodette/om) for the UI
* [core.async](https://github.com/clojure/core.async) for messaging (including across web sockets)

Not much else. Basically, all messaging between all components goes through
`core.async` and that way, all the stuff about where the message is going, etc.
is abstracted away. Boom, your app is just about message passing, not about
client/server, HTTP/REST, etc. It's just about messages.

And there you have it.

# Workflow



## License

Spiffy is dual licensed under the Eclipse Public License,
just like Clojure, and the LGPL 2, your choice.

A side note about licenses... my goal with the license is to
make sure the code is usable in a very wide variety of projects.
Both the EPL and the LGPL have contribute-back clauses. This means
if you make a change to Spiffy, you have to make your changes
public. But you can use Spiffy in any project, open or closed.
Also, the dual license is meant to allow Spiffy to be used in
GPL/AGPL projects... and there are some "issues" between the FSF
and the rest of the world about how open the EPL, the Apache 2, etc.
licenses are. I'm not getting caught in that deal.

(c) 2014 WorldWide Conferencing, LLC

