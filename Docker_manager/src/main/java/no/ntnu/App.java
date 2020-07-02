package no.ntnu;

import no.ntnu.DockerInterface.ImageBuilder;
import no.ntnu.sql.PsqlInterface;

import java.util.Arrays;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        try {
            Integer[] que = PsqlInterface.getSortedWaitingQue();


        } catch (Exception e){
            e.printStackTrace();
        }

    }
}
