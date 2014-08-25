# A Spiffy place to start

Okay... so it's kinda tough to start a web project in Clojure.
Sure, there's [Pedestal](https://github.com/pedestal/pedestal),
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

A big part of using Clojure is a simple, fast workflow where you
can make a change and see the change in real time. This workflow
is part of Spiffy.

You need to start 2 REPL sessions... one for the server part of your
code and the other for the client part of your code. I do this
via emacs and [Cider](https://github.com/clojure-emacs/cider).

I start two emacs sessions:

     emacs src/cljs/spiffy/main.cljs &
      
     emacs src/clj/spiffy/server.clj &


And then I start a compilation session for the [CLJX](https://github.com/lynaghk/cljx)
and ClojureScript code:

     lein pdo cljx auto, cljsbuild auto dev

That kicks off parallel `cljx` and `cljs` compilers... so any
change to the Cljx files will trigger a ClojureScript compilation
and that means each time we reload the web page, we'll have the
most recent version of the code.

In one of the emacs sessions, I change the theme with `M-x` `load-theme`.
In the case of the ClojureScript emacs session, the theme is light and
in the Clojure session, the theme is dark.

In each of the emacs sessions, I start a REPL via `C-c M-j` or `M-x` `cider-jack-in`.

Once the REPLs are started, in the ClojureScript session, I start the
ClojureScript REPL:

	user> (require 'spiffy.server)
	nil
	user> (spiffy.server/run-cljs-repl)
	Type `:cljs/quit` to stop the ClojureScript REPL
	nil
	cljs.user>

And in the Clojure session, I start the web server:

	user> (require 'spiffy.server)
	nil
	user> (ns spiffy.server)
	nil
	spiffy.server> (start-server)
	Running
	#<server$run_server$stop_server__8801 org.httpkit.server$run_server$stop_server__8801@f9008b0>
	spiffy.server> 

Then I load the page at http://localhost:8080 and boom... I have a web page
hooked into both REPLs.

We can test the ClojureScript REPL:

	cljs.user> (ns spiffy.main)
	nil
	spiffy.main> (swap! app-state assoc :text "Wombat")

And boom, in the browser, we see the text in the main component change.


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

