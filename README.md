# JavaBrowser
Javafx browser made for bypassing proxy web filters.

This browser has a feature referenced in the code as 'HyperDodge'. This routes the traffic through a proxy, at the expense of performance and some reliability.

If HyperDodge is disabled, the browser will behave normally, still bypassing computer-level blocks (read: Sophos, etc) while still being blocked at the wifi level. Other wifis which aren't blocked (home, hotsposts, etc) won't block it anyway, so having HyperDodge disabled is better in instances such as those.

I made this a year ago, so my code formatting is exceptionally bad. Maybe someday I'll get around to fixing the formatting, but for now it's going to stay like this.

The code also refers to a fallback method of bypassing filters. This is currently unimplemented, but the code is still there. What it does is set up a sort of pseudo-proxy. It sends the url over TCP to the server, which retrieves the HTML and responds with it. The browser then loads that HTML. However, this was slow, unreliable, and didn't catch links. (It only worked when typing into the address bar) The function setUpSocket() and a majority of the load() function are vestigial from that method.

In the main() method, the if statement if(approved || true) is meant so that anyone who downloads it off github can run it. It does not suit my purposes to have this browser to be able to be run by everybody, so I authenticate the username. (the organization pre-decides usernames)
