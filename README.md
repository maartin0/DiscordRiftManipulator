# Discord Rift Manipulator
Discord Rift Manipulator is a discord bot written in Java using [DV8FromTheWorld/JDA](https://github.com/DV8FromTheWorld/JDA). It enables you to create 'Rifts' which link text channels across guilds together. 

## Invite
You can invite the bot to your server using [this](https://discord.com/api/oauth2/authorize?client_id=888800676535218237&permissions=536996880&scope=bot%20applications.commands) url, or copy and paste the URL below:  
[https://discord.com/api/oauth2/authorize?client_id=888800676535218237&permissions=536996880&scope=bot%20applications.commands](https://discord.com/api/oauth2/authorize?client_id=888800676535218237&permissions=536996880&scope=bot%20applications.commands)

## Usage
### Commands
`/create <name> <description>` - Creates a new rift with the supplied name and description. (Permission Requirement: Administrator)  
`/join <token>` - Joins an existing rift with the supplied token. (Permission Requirement: Administrator)  
`/leave` - Leaves the rift and removes it from the current channel. (Permission Requirement: Administrator)  

`/modify <prefix> <description> <invite_code>` - Modifies local channel settings. (Permission Requirement: Administrator)  
`/set_prefix <prefix>` - Sets the channel prefix. (Permission Requirement: Administrator)  
`/set_description <description>` - Sets the channel description. (Permission Requirement: Administrator)  
`/set_invite <invite_code>` - Sets the guild invite code. (Permission Requirement: Administrator)  

`/delete-message <message_id>` - Deletes the supplied message from every channel on the rift. (Permission Requirement: Manage Messages)  

`/global_modify <name> <description>` - Modifies global channel settings. (Permission Requirement: Administrator; This command only works on the server that created the rift)  

### Global Variables
**name** - The name of the rift.  
**description** - The description of the rift.  
**token** - The token which allows other guilds to join the rift.  

### Local Variables
**prefix** - The prefix of the channel. This is sent to every message from that channel.  
**description** - The description of the channel. This is only used if you need a custom description in the channel topic. This is not sent to any other servers.  
**invite_code** - If you set this variable, it will show in the description of all servers after your server name. This should be the invite *code* rather than *url*. This prevents non-discord URLs being added to the description.  

## Self Hosting
Feel free to host the bot yourself. A pre-compiled jar can be found in the releases page.

#### Initial Setup
After downloading the latest jar, run it with JRE 15 or later. A **config.json** file should appear. The minimum requirement is the bot token, which has the key token.

#### Configuration
**token** - The bot token.

**prefix_regex** - A regex that matches allowed characters in the prefix. Default is Extended ASCII.
**debug_administrators** - Allows the provided user ids to run commands on any server, even if they do not have the correct permissions. Useful for debugging on larger rifts.

**update_commands** - Usually you shouldn't need to touch this. Turning it to true will cause discord to refresh the global commands once.

#### Compilation
If you're that kind of person, feel free to compile the source yourself. The included maven `pom.xml` should be all you need to build it.

## Contribute
If you find any issues feel free to open a new [issue](https://github.com/infinitewiggles/DiscordRiftManipulator/issues), or submit a new [pull request](https://github.com/infinitewiggles/DiscordRiftManipulator/pulls). Thanks for helping!

## Patreon 
To pay for better hosting a patreon page has been requested. You can donate [here](https://www.patreon.com/riftmanipulator) to fund the bot hosting & development, thanks!
