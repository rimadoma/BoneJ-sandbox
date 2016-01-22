package org.bonej.common;

/**
 * MultiThreading copyright 2007 Stephan Preibisch
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */


import ij.Prefs;

/**
 * MultiThreader utility class for convenient multithreading of ImageJ plugins
 *
 * @author Stephan Preibisch
 * @author Michael Doube
 *
 * @see <p>
 *      <a href="http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD"
 *      >http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/
 *      MultiThreading.java;hb=HEAD</a>
 */
public class MultiThreader {
    public static void startTask(Runnable run) {
        Thread[] threads = newThreads();

        for (int thread = 0; thread < threads.length; ++thread)
            threads[thread] = new Thread(run);
        startAndJoin(threads);
    }

    public static Thread[] newThreads() {
        int nThreads = Prefs.getThreads();
        return new Thread[nThreads];
    }

    public static void startAndJoin(Thread[] threads) {
        for (Thread thread : threads) {
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.start();
        }

        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }
}
