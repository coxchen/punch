# punch

## Prerequisites

- Download the [Leiningen script](https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein) and put it on the $PATH
- git clone this repository
- cd into the repository folder and execute `bower install` to fetch *semantic-ui*

## Development

Open a terminal and type `lein repl` to start a Clojure REPL
(interactive prompt).

In the REPL, type

```clojure
(go)
(cljs-repl)
```

The call to `(go)` starts the Figwheel server at port 3449, which takes care of
live reloading ClojureScript code and CSS, and the app server at port 10555 
which forwards requests to the http-handler you define.

Running `(cljs-repl)` starts the Figwheel ClojureScript REPL. Evaluating
expressions here will only work once you've loaded the page, so the browser can
connect to Figwheel.

When you see the line `Successfully compiled "resources/public/app.js" in 21.36
seconds.`, you're ready to go. Browse to `http://localhost:10555` and enjoy.

**Attention: It is not needed to run `lein figwheel` separately. Instead `(go)`
launches Figwheel directly from the REPL**

### Lighttable

[Lighttable](http://lighttable.com/) provides a tighter integration for live coding with an inline
browser-tab. Rather than evaluating cljs on the command line with the Figwheel
REPL, you can evaluate code and preview pages inside Lighttable.

Steps: After running `(go)`, open a browser tab in Lighttable. Open a cljs file
from within a project, go to the end of an s-expression and hit Cmd-ENT.
Lighttable will ask you which client to connect. Click 'Connect a client' and
select 'Browser'. Browse to [http://localhost:10555](http://localhost:10555)

View LT's console to see a Chrome js console.

Hereafter, you can save a file and see changes or evaluate cljs code (without
saving a file).

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

## Chestnut

Created with [Chestnut](http://plexus.github.io/chestnut/) 0.16.0 (67651e9d).
