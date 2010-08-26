package sc.plugin_schaefchen.simple;


import java.lang.reflect.InvocationTargetException;

import sc.plugin_schaefchen.AbstractClient;
import sc.plugin_schaefchen.IGameHandler;

/**
 * Erlaubt es verschiedene Logiken zu verwenden und eine davon auszuwählen
 * und Instanzen dieser Logik zu erzeugen
 * 
 * @author and
 */
public enum LogicFactory {
	// Verfügbare Taktiken (Implementierungen des IGameHandler) müssen hier 
	// eingetragen wie im Beispiel eingetragen und ihre Klasse angegeben werden
	RANDOM(RandomLogic.class),
	GREEDY(GreedyLogic.class),
	KARA_SIMPLE(KaraLogic.class),
	// Die Logik die gewählt wird, wenn kein passender Eintrag zu der Eingabe gefunden wurde:
	DEFAULT(RandomLogic.class);

	private Class logic;
	
	
	private LogicFactory(Class chosenLogic){
		logic = chosenLogic;
	}
	
	/**
	 * Erstellt eine Logik-Instanz und gibt diese zurück
	 * 
	 * @param client
	 * 		Der aktuelle Client
	 * @return
	 * 		Eine Instanz der gewaehlten Logik
	 * @throws Exception
	 * 		Wenn etwas schief gelaufen ist und keine Instanz erstellt werden konnte, 
	 * 		wird eine Exception geworfen!
	 */
	public IGameHandler getInstance(AbstractClient client) throws Exception{
		System.out.println("Erzeuge Instanz von: "+name());
		return (IGameHandler) logic.getConstructor(client.getClass()).newInstance(client);
	}
	
	
	
}