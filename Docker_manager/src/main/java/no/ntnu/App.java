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
        //
        //  HUSK OG HANDLE TING SOM STåR SOM RUNNING VED POWERON
        //
        try {
            while (true){



                Integer[] que = PsqlInterface.getSortedWaitingQue();

                for (Integer i: que) {

                }



                Thread.sleep(1000);
            }
        } catch (Exception e){
            e.printStackTrace();
        }


    }
}
