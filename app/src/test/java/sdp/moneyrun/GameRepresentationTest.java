package sdp.moneyrun;

import org.junit.Test;

import sdp.moneyrun.game.GameRepresentation;
import sdp.moneyrun.map.LocationRepresentation;

import static org.junit.Assert.assertEquals;

public class GameRepresentationTest {
    @Test
    public void constructionOfGameRepresentationWorks(){
        LocationRepresentation lr = new LocationRepresentation(0, 1);
        GameRepresentation gr = new GameRepresentation("0", "game", 0, 16, lr);

        assertEquals(1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructionOfGameRepresentationFailsWhenNullId(){
        LocationRepresentation lr = new LocationRepresentation(0, 1);
        GameRepresentation gr = new GameRepresentation(null, "game", 0, 16, lr);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructionOfGameRepresentationFailsWhenNullName(){
        LocationRepresentation lr = new LocationRepresentation(0, 1);
        GameRepresentation gr = new GameRepresentation("0", null, 0, 16, lr);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructionOfGameRepresentationFailsWhenNullLocation(){
        GameRepresentation gr = new GameRepresentation("0", "game", 0, 16, null);
    }

    @Test
    public void getGameIdReturnsCorrectValue(){
        LocationRepresentation lr = new LocationRepresentation(0, 1);
        GameRepresentation gr = new GameRepresentation("0", "game", 0, 16, lr);

        assertEquals(gr.getGameId(), "0");
    }

    @Test
    public void getNameReturnsCorrectValue(){
        LocationRepresentation lr = new LocationRepresentation(0, 0);
        GameRepresentation gr = new GameRepresentation("0", "game", 0, 16, lr);

        assertEquals(gr.getName(), "game");
    }

    @Test
    public void getPlayerCountReturnsCorrectValue(){
        LocationRepresentation lr = new LocationRepresentation(0, 0);
        GameRepresentation gr = new GameRepresentation("0", "game", 0, 16, lr);

        assertEquals(gr.getPlayerCount(), 0);
    }

    @Test
    public void getMaxPlayerCountReturnsCorrectValue(){
        LocationRepresentation lr = new LocationRepresentation(0, 0);
        GameRepresentation gr = new GameRepresentation("0", "game", 0, 16, lr);

        assertEquals(gr.getMaxPlayerCount(), 16);
    }

    @Test
    public void getStartLocationReturnsCorrectValue(){
        LocationRepresentation lr = new LocationRepresentation(0, 0);
        GameRepresentation gr = new GameRepresentation("0", "game", 0, 16, lr);

        assertEquals(gr.getStartLocation(), lr);
    }
}
