# Pet Selection Menu
Where players store their active pets
- Define slot(s) where players can place pets
- Define permissions to go with these slots to only unlock a certain number of slots
- The position of stored pets persists
- Also contains item(s) which allow players to update message settings

## Admin Editing
Admins are able to access a player's selection menu and modify its contents
- **Supports offline player editing**
- Due to concurrency issues, only one player can edit the pets at a time. This is determined by the player who opened the menu earlier
- Right-clicking allows you to bypass the pet cooldown
- Editing the menu of an online player will push any potion effect changes to the player

# Pet Menu System
Now the fun stuff! You are able to define menus which contain the pets you have created

## Item Types
Four item types are supported. Check out the example file for proper syntax
- `BACK` A simple item that brings the player back to the main menu when clicked (unable to be put in the main menu)
- `ITEM` Contains text. Does nothing when clicked
- `LINK` Define an item that opens another menu when clicked
- `PET` Places the pet's "menu item" in this location

## Menus Explained
- The main menu is always opened first when running the menu open command. Back buttons always go back to this menu
- You are able to define submenus that are accessible via `LINK` items
- This menu system lends itself to organization, so keep it organized for your players
