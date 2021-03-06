/**
 * Created by kirillbokov on 06.09.16.
 */

    import com.fasterxml.jackson.annotation.JsonProperty;

    public class GameResponseItem {
        @JsonProperty private final String name;
        @JsonProperty private final int attack;
        @JsonProperty private final int armor;
        @JsonProperty private final int agility;
        @JsonProperty private final int endurance;

        public GameResponseItem(
                @JsonProperty("name") final String name,
                @JsonProperty("attack") final int attack,
                @JsonProperty("armor") final int armor,
                @JsonProperty("agility") final int agility,
                @JsonProperty("endurance") final int endurance
        ) {
            this.name = name;
            this.attack = attack;
            this.armor = armor;
            this.agility = agility;
            this.endurance = endurance;
        }

        public String getName() {
            return name;
        }

        public int getAttack() {
            return attack;
        }

        public int getArmor() {
            return armor;
        }

        public int getAgility() {
            return agility;
        }

        public int getEndurance() {
            return endurance;
        }

        @Override
        public String toString() {
            return "name: " + name
                    + ", attack: " + attack
                    + ", armor: " + armor
                    + ", agility: " + agility
                    + ", endurance: " + endurance;
        }
    }

