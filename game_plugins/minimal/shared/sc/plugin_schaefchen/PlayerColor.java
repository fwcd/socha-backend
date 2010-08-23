package sc.plugin_schaefchen;

/**
 * abstrakte spielerfarben die echte farben liefern
 * 
 * @author tkra
 * 
 */
public enum PlayerColor {

	PLAYER1, PLAYER2;

	/**
	 * liefert die spielerfarbe des gegners dieses spielers
	 */
	public PlayerColor oponent() {
		PlayerColor result = null;
		switch (this) {
		case PLAYER1:
			result = PlayerColor.PLAYER2;
			break;

		case PLAYER2:
			result = PlayerColor.PLAYER1;
			break;
		}

		return result;
	}

}