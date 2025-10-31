This is a simple Velocity plugin to help work around low-end hardware limitations of a self hosted Minecraft server.

<h1>What it does</h1>
Looks at all the connections, and redirects them to the main server, after a full connection has been established to prevent the 30 second timeout

<h1>Example Setup</h1>
This plugin is only useful in a rather specific enviroment that has:
<ul>
  <li>A Velocity Proxy</li>
  <li>Impulse configured on the velocity proxy</li>
  <li>A main/primary server that takes more than 30 seconds to start</li>
  <li>A fast starting or always on server (like a limbo server)</li>
</ul>

<h1>To Use</h1>

1. Install the plugin
2. Change the velocity config to try the limbo server first

*velocity.toml*

```toml 
# In what order we should try servers when a player logs in or is kicked from a server.
try = [
    "limbo",
    "lobby"
]
```

3. Start the server to generate the default config
4. Update the config as needed

*/plugins/serverautojoin/config.toml*

``` toml
#name of the server that players initially join, a limbo or hub server of some kind
entry_server = "limbo"
#name of the server that will take too long to startup
redirect_server = "lobby"
```