import java.util.*;
import java.io.*;
import dungeon.*;
import dungeon.exceptions.*;

/* TODO: write tests */
/* TODO: place abbreviated actions closer to 0 */

public class DungeonProtocol {
  public static final double VERSION = 0.1;

  public static enum Action {
    MOVE, TAKE, GIVE, LOOK, EXITS, SAY, YELL, WHISPER, USE, HELP, QUIT
  }

  public static final String[] actionTokens = {
    "m", "move", "go", "walk", "ride",
    "t", "take", "get",
    "g", "give",
    "l", "look", "describe",
    "e", "exits",
    "s", "say", "talk",
    "y", "yell", "shout",
    "w", "whisper",
    "u", "use",
    "h", "help",
    "q", "quit"
  };

  public static final Action[] actionEnums = {
    Action.MOVE, Action.MOVE, Action.MOVE, Action.MOVE, Action.MOVE,
    Action.TAKE, Action.TAKE, Action.TAKE,
    Action.GIVE, Action.GIVE,
    Action.LOOK, Action.LOOK, Action.LOOK,
    Action.EXITS, Action.EXITS,
    Action.SAY, Action.SAY, Action.SAY,
    Action.YELL, Action.YELL, Action.YELL,
    Action.WHISPER, Action.WHISPER,
    Action.USE, Action.USE,
    Action.HELP, Action.HELP,
    Action.QUIT, Action.QUIT
  };

  /*
   * This method is called by the server when it recieves a string
   * from the client over the network and attempts to verify it against
   * the Dungeon protocol. If there are any errors, or if the player
   * is not authorized to take the specified action, the universe is
   * not altered and an error string is sent back.
   */
  public static String process(Player p, String input) {
    if (input == null)
      throw new IllegalArgumentException("recieved null");

    /* Ignore blank input */
    if (input.isEmpty()) return "";

    /* Split into tokens */
    String[] tokens = input.split("\\s");
    
    int k = -1;
    /* Parse the action or return an error */
    for (int i = 0; i < actionTokens.length; i++) {
      if (tokens[0].equals(actionTokens[i])) {
        k = i;
        break;
      }
    }

    if (k == -1)
      return ">>> Unsure what is meant by '" + tokens[0] + "'.\n" +
             ">>> Try 'help' to get a list of actions.";

    switch (actionEnums[k]) {
      case MOVE:
        if (p.wantsQuit) p.wantsQuit = false;

        String str = "";

        try {
          Space dest = p.here().to(tokens[1]);
          
          if (dest instanceof Room) {
            p.move((Room)dest);
          }

          if (dest instanceof Door) {
            Door d = (Door)dest;

            if (d.isLocked()) {
              /* Does the player have the correct key? */
              Iterator<Item> iter = p.getInventoryIterator();

              while (iter.hasNext()) {
                Item i = iter.next();

                if (!(i instanceof Key))
                  continue;

                if (d.keyFits((Key)i)) {
                  str += "I used the key to unlock the door and closed it " +
                         "behind me.";
                  p.move((Room)d.to(tokens[1]));
                  break;
                }
              }
              
            } else {
              str += "I slipped through the unlocked door.\n";
              p.move((Room)d.to(tokens[1]));
            }
          }

          return str + p.here().describe();

        } catch (NoSuchDirectionException e) {
          return ">>> That's not a way out of here.";
        } catch (ArrayIndexOutOfBoundsException e) {
          return ">>> Specify a direction in which to move.";
        }
        
      case TAKE:
        if (p.wantsQuit) p.wantsQuit = false;
        return "got take";
      case GIVE:
        if (p.wantsQuit) p.wantsQuit = false;
        return "got give";
      case LOOK:
        if (p.wantsQuit) p.wantsQuit = false;
        
        if (tokens.length == 1)
          return p.here().describe();

        try {
          return p.here().getItemByName(tokens[1]).describe();
        } catch (NoSuchItemException e) {
          return ">> No such item '" + tokens[1] + "'.";
        }

        // TODO: check for players in this room

      case EXITS:
        if (p.wantsQuit) p.wantsQuit = false;
        return "go exits";
      case SAY:
        if (p.wantsQuit) p.wantsQuit = false;
        return "got say";
      case YELL:
        if (p.wantsQuit) p.wantsQuit = false;
        return "got yell";
      case WHISPER:
        if (p.wantsQuit) p.wantsQuit = false;
        return "got whisper";
      case USE:
        if (p.wantsQuit) p.wantsQuit = false;
        return "got use";
      case HELP:
        if (p.wantsQuit) p.wantsQuit = false;
        return usage();
      case QUIT:
        if (p.wantsQuit) {
          p.wantsQuit = false;
          DungeonServer.universe.retire(p);
          
          /* Indicate to the connection thread that we want to disconnect */
          return null;
        } else {
          p.wantsQuit = true;
          return ">>> Are you sure you want to quit?\n" +
                 ">>> Type 'q' or 'quit' again to quit.";
        }

    }

    return "!!! Unexpected server error.";
  }

  private static String usage() {
    return "ACTION               OBJECT        INDIRECT OBJECT\n" +
           "[{m,move,go,walk}]   <direction>\n" +
           "{t,take,get}         <object>\n" +
           "{g,give}             <object>      to <player>\n" +
           "{l,look,describe}    [<object>]\n" +
           "{e,exits}\n" +
           "{s,say,talk}         [<string>]\n" +
           "{y,yell,shout}       <string>\n" +
           "{w,whisper}          <string>      to <player>\n" +
           "{u,use}              <object in inventory>\n" +
           "{h,help}\n" +
           "{q,quit}";
  }
}