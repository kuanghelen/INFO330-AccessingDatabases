import java.sql.*;
import java.util.ArrayList;

public class TeamAnalyzer {
    // All the "against" column suffixes:
    static String[] types = {
        "bug","dark","dragon","electric","fairy","fight",
        "fire","flying","ghost","grass","ground","ice","normal",
        "poison","psychic","rock","steel","water"
    };

    public static void main(String... args) throws Exception {
        // Take six command-line parameters
        if (args.length < 6) {
            print("You must give me six Pokemon to analyze");
            System.exit(-1);
        }

        // This bit of JDBC magic I provide as a free gift :-)
        // The rest is up to you.
        try (Connection con = DriverManager.getConnection("jdbc:sqlite:pokemon.sqlite")) {
            // find all types of a pokemon given the pokedex number
            String type_id_sql = "SELECT t.* " +
                                 "FROM pokemon AS p, pokemon_type AS pt, type AS t " +
                                 "WHERE p.pokedex_number = ? " +
                                 "AND p.id = pt.pokemon_id " +
                                 "AND pt.type_id = t.id";
            
            // find all against information about a pokemon given its two type ids
            String against_sql = "SELECT a.* " +
                                 "FROM against AS a " +
                                 "WHERE type_source_id1 = ? " +
                                 "AND type_source_id2 = ? ";
            
            // find the name of the pokemon
            String name_sql = "SELECT name from pokemon WHERE pokedex_number = ?";
            
            // extra credit: find the pokedex number associated with a given pokemon name
            String pokedex_sql = "SELECT pokedex_number FROM pokemon WHERE name = ?";

            // use PreparedStatements to prevent SQL injections
            PreparedStatement typeIdStmt = con.prepareStatement(type_id_sql);
            PreparedStatement againstStmt = con.prepareStatement(against_sql);
            PreparedStatement nameStmt = con.prepareStatement(name_sql);
            PreparedStatement pokedexStmt = con.prepareStatement(pokedex_sql);
            
            for (String arg : args) {
                // extra credit: convert pokemon name to pokedex number if necessary
                int argToInt = -1; // will store pokedex number
                try {
                    argToInt = Integer.parseInt(arg); // was given a String pokedex number, convert it into an int
                } catch (Exception e) { // was not given pokedex number; must find the pokedex number
                    try {
                        pokedexStmt.setString(1, arg);
                        ResultSet rs = pokedexStmt.executeQuery();
                        if (rs.next()) {
                            argToInt = rs.getInt("pokedex_number");
                        }
                    } catch (Exception ee) {
                        ee.printStackTrace();
                    }
                }

                print("Analyzing " + argToInt);

                String pokemonName = ""; // the pokemon's name
                ArrayList<Integer> typesIdArr = new ArrayList<Integer>();   // list of type ids (will be size 2)
                ArrayList<String> typesNameArr = new ArrayList<String>();   // list of type names (will be size 2)
                ArrayList<Double> againstInfo = new ArrayList<Double>();    // list of against_x values (will be size 18)
                try {
                    // determine the list of type ids and type names given pokedex number
                    typeIdStmt.setInt(1, argToInt);
                    ResultSet typeRS = typeIdStmt.executeQuery();
                    while (typeRS.next()) {
                        typesIdArr.add(typeRS.getInt("id"));
                        typesNameArr.add(typeRS.getString("name"));
                    }
                    typeRS.close();
                    
                    // determine the list of against_x values given type1 id and type2 id
                    againstStmt.setInt(1, typesIdArr.get(0));
                    againstStmt.setInt(2, typesIdArr.get(1));
                    ResultSet againstRS = againstStmt.executeQuery();
                    if (againstRS.next()) {
                        for (int i = 0; i < types.length; i++) {
                            againstInfo.add(againstRS.getDouble("against_" + types[i]));
                        }
                    }
                    againstRS.close();
                    
                    // determine the pokemon's name given pokedex number
                    nameStmt.setInt(1, argToInt);
                    ResultSet nameRS = nameStmt.executeQuery();
                    if (nameRS.next()) {
                        pokemonName = nameRS.getString("name");
                    }
                    nameRS.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }

                // determine the list of strong against and weak against types
                ArrayList<String> strong = new ArrayList<String>();
                ArrayList<String> weak = new ArrayList<String>();
                for (int i = 0; i < againstInfo.size(); i++) {
                    Double val = againstInfo.get(i);
                    if (val < 1) {
                        strong.add("'" + types[i] + "'");
                    } else if (val > 1) {
                        weak.add("'" + types[i] + "'");
                    }
                }
                
                // print individual pokemon information
                print(pokemonName + " (" + typesNameArr.get(0) + " " + typesNameArr.get(1) + ") " + 
                                    "is strong against " + strong + " but weak against " + weak);
            }

            String answer = input("Would you like to save this team? (Y)es or (N)o: ");
            if (answer.equalsIgnoreCase("Y") || answer.equalsIgnoreCase("YES")) {
                String teamName = input("Enter the team name: ");

                // Write the pokemon team to the "teams" table
                print("Saving " + teamName + " ...");
            }
            else {
                print("Bye for now!");
            }
        }        
    }

    /*
     * These are here just to have some symmetry with the Python code
     * and to make console I/O a little easier. In general in Java you
     * would use the System.console() Console class more directly.
     */
    public static void print(String msg) {
        System.console().writer().println(msg);
    }

    /*
     * These are here just to have some symmetry with the Python code
     * and to make console I/O a little easier. In general in Java you
     * would use the System.console() Console class more directly.
     */
    public static String input(String msg) {
        return System.console().readLine(msg);
    }
}
