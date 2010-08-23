package sc.plugin_schaefchen;

import java.util.List;


import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias(value="sit:welcome")
public class WelcomeMessage
{
	@XStreamAsAttribute
	private PlayerColor color;

	// in der welcomemessage die statischen knoteninformationen uebertragen 
	@SuppressWarnings("unused")
	@XStreamImplicit(itemFieldName = "node")
	private List<Node> nodes = BoardFactory.nodes;
	
	public WelcomeMessage(PlayerColor c)
	{
		color = c;
	}
	
	public PlayerColor getYourColor()
	{
		return color;
	}
}