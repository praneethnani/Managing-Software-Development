
/*
 * Comparing mp3 and wav files to see if one file is derived from another file
 * In case both the path specs contain directories, each WAV or mp3 in 
 * directory1 will be compared against each WAV or mp3 file in directory2.
 * All other file types will be ignored
 *
 */
import java.io.*;
import java.util.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Arc5500 {
    // pathspecType will hold type of pathname.
    // For Example {"x1.wav": "file". "A6": "directory"}
    private static HashMap<String, String> pathspecType = new HashMap<String, String>();
    private static HashMap<String, List<Boolean>> songs = new HashMap<String, List<Boolean>>();
    private static HashMap<String, Float> computedMatches = new HashMap<String, Float>();
    private static HashMap<String, Float> computedMismatches = new HashMap<String, Float>();
    private static HashMap<String, Double> computedWholeFileRMS = new HashMap<String, Double>();
    private static String fileName1;
    private static String fileName2;
    // private static int sample_rate=11025;
    // List of valid tokens.
    public static final List<String> ValidPathSpecTokens = Arrays.asList("-f",
            "--file", "-d", "--dir");

    public static void main(String[] args) {
        if (args.length != 4) {
            System.err
                    .println("ERROR: Unexpected number of command line arguments.");
            System.exit(1);
        }
        if (!(ValidPathSpecTokens.contains(args[0]) && ValidPathSpecTokens
                .contains(args[2]))) {
            System.err.println("ERROR: Incorrect pathspec.");
            System.exit(1);
        }
        if (args[0].equals("-f") || args[0].equals("--file")) {
            pathspecType.put(args[1], "file");
        } else {
            pathspecType.put(args[1], "directory");
        }
        if (args[2].equals("-f") || args[2].equals("--file")) {
            pathspecType.put(args[3], "file");
        } else {
            pathspecType.put(args[3], "directory");
        }
        checkValidity(args[1], args[3]);
    }

    /*
     * Given two pathnames, checks if each pathname corresponds to the format
     * specified in the problem statement. Uses helper functions to determine
     * whether the given path contains files / directories exits if the given
     * path is invalid
     */
    private static void checkValidity(String pathname1, String pathname2) {
        if (checkExistance(pathname2) && checkExistance(pathname1)) {
            checkMatchForFiles(pathname1, pathname2);
            System.exit(0);
        } else {
            System.exit(1);
        }
    }

    /*
     * Given a pathname, checks whether the path actually exists in the file
     * system. Produces an error when the given pathname doesn't correspond to
     * valid file or a valid directory
     * 
     * In case the given pathname corresponds to a directory, this functions
     * checks whether it contains any sub directories
     */
    private static boolean checkExistance(String pathname) {
        File f = new File(pathname);
        Boolean doesExists;
        Boolean isCorrectFormat = false;
        if (pathspecType.get(pathname).equals("file")) {
            doesExists = f.isFile();
            if (!doesExists) {
                System.err.println("ERROR: File " + pathname
                        + " does not exists.");
                isCorrectFormat = false;
            } else {
                isCorrectFormat = checkFormat(f);
            }
        } else {
            doesExists = f.isDirectory();
            if (!doesExists) {
                System.err.println("ERROR: Directory " + pathname
                        + " does not exists.");
                isCorrectFormat = false;
            } else {
                File[] listOfFiles = f.listFiles();
                for (int i = 0; i < listOfFiles.length; i++) {
                    if (listOfFiles[i].isDirectory()) {
                        System.err.println("ERROR: Sub-Directory "
                                + listOfFiles[i] + " exists.");
                        isCorrectFormat = false;
                    } else {
                        isCorrectFormat = checkFormat(listOfFiles[i]);
                    }
                }
            }
        }
        return isCorrectFormat;
    }

    /*
     * Given a file, returns True if the file is a WAV Audio format or a valid
     * MP3 format else, returns false.
     */
    private static boolean checkFormat(File f) {
        Boolean isWave = checkWave(f);
        Boolean isMP3 = checkMP3(f);
        if (isWave) {
            return true;
        } else if (isMP3) {
            return true;
        }
        System.err.println("ERROR: Unsuported Audio Format of " + f.toString());
        return false;
    }

    /*
     * Given two pathspecs, returns all valid matches between the given set of
     * file(s). A pathspec can be a file or a directory containing MP3 and WAV
     * files.
     * 
     * We choose WAV format as the canonical format for all our comparisons. So,
     * we are checking if the file is an MP3 and converting the MP3 file to a
     * WAV file before checking for matches.
     */
    private static void checkMatchForFiles(String fs1, String fs2) {
        File f1 = new File(fs1);
        File f2 = new File(fs2);

        // Pathspec1 and pathspec2 are both files
        if ((pathspecType.get(fs1).equals("file"))
                && (pathspecType.get(fs2).equals("file"))) {
            if (checkMP3(f1)) {
                f1 = convertMP3Mono(f1);
                f1 = convertMP3(f1);
            } else {
                f1 = convertWAV(f1);
                f1 = convertMP3Mono(f1);
                f1 = convertMP3(f1);
            }
            if (checkMP3(f2)) {
                f2 = convertMP3Mono(f2);
                f2 = convertMP3(f2);
            } else {
                f2 = convertWAV(f2);
                f2 = convertMP3Mono(f2);
                f2 = convertMP3(f2);
            }
            checkFileMatch(f1, f2);
        }

        // Pathspec1 is a directory and pathspec2 is a file
        else if ((pathspecType.get(fs1).equals("directory"))
                && (pathspecType.get(fs2).equals("file"))) {
            File[] listOfFiles = f1.listFiles();
            if (checkMP3(f2)) {
                f2 = convertMP3Mono(f2);
                f2 = convertMP3(f2);
            } else {
                f2 = convertWAV(f2);
                f2 = convertMP3Mono(f2);
                f2 = convertMP3(f2);
            }
            for (int i = 0; i < listOfFiles.length; i++) {

                if (checkMP3(listOfFiles[i])) {
                    listOfFiles[i] = convertMP3Mono(listOfFiles[i]);
                    listOfFiles[i] = convertMP3(listOfFiles[i]);
                } else {
                    listOfFiles[i] = convertWAV(listOfFiles[i]);
                    listOfFiles[i] = convertMP3Mono(listOfFiles[i]);
                    listOfFiles[i] = convertMP3(listOfFiles[i]);
                }
                checkFileMatch(listOfFiles[i], f2);
            }
        }

        // pathspec1 is a file and pathspec2 is a directory
        else if ((pathspecType.get(fs1).equals("file"))
                && (pathspecType.get(fs2).equals("directory"))) {
            File[] listOfFiles = f2.listFiles();
            fileName1 = f1.getName();
            if (checkMP3(f1)) {
                f1 = convertMP3Mono(f1);
                f1 = convertMP3(f1);
            } else {
                f1 = convertWAV(f1);
                f1 = convertMP3Mono(f1);
                f1 = convertMP3(f1);
            }
            for (int i = 0; i < listOfFiles.length; i++) {
                File file = listOfFiles[i];
                fileName2 = file.getName();
                if (checkMP3(listOfFiles[i])) {
                    listOfFiles[i] = convertMP3Mono(listOfFiles[i]);
                    listOfFiles[i] = convertMP3(listOfFiles[i]);
                } else {
                    listOfFiles[i] = convertWAV(listOfFiles[i]);
                    listOfFiles[i] = convertMP3Mono(listOfFiles[i]);
                    listOfFiles[i] = convertMP3(listOfFiles[i]);
                }
                checkFileMatch(f1, listOfFiles[i]);
            }
        }

        // finally when both pathspecs are directories
        else {
            File[] listOfFiles1 = f1.listFiles();
            File[] listOfFiles2 = f2.listFiles();
            File file1;
            File file2;
            for (int i = 0; i < listOfFiles1.length; i++) {
                file1 = listOfFiles1[i];
                fileName1 = file1.getName();
                if (checkMP3(file1)) {
                    file1 = convertMP3Mono(file1);
                    file1 = convertMP3(file1);
                } else {
                    file1 = convertWAV(file1);
                    file1 = convertMP3Mono(file1);
                    file1 = convertMP3(file1);
                }
                for (int j = 0; j < listOfFiles2.length; j++) {
                    file2 = listOfFiles2[j];
                    fileName2 = file2.getName();
                    if (checkMP3(file2)) {
                        file2 = convertMP3Mono(file2);
                        file2 = convertMP3(file2);
                    } else {
                        file2 = convertWAV(file2);
                        file2 = convertMP3Mono(file2);
                        file2 = convertMP3(file2);
                    }
                    checkFileMatch(file1, file2);
                }
            }
        }
    }

    /*
     * Given two files, function reach through each sample for file1 and file2
     * to check if there are any matches between these two files Header of file
     * will be stripped before comparing samples from one file against the other
     * file
     */

    private static void checkFileMatch(File f1, File f2) {
        int size1 = (int) f1.length();
        int size2 = (int) f2.length();
        byte[] buf1 = new byte[size1];
        byte[] buf2 = new byte[size2];
        List<Boolean> largerFileRMS;
        List<Boolean> smallerFileRMS;
        String largerFileName;
        String smallerFileName;
        FileInputStream fInput1;
        FileInputStream fInput2;
        FileInputStream fInput11;
        FileInputStream fInput22;
        try {
            fInput1 = new FileInputStream(f1);
            fInput11 = new FileInputStream(f1);
            fInput11.read(buf1);
            fInput1.skip(44);
            fInput2 = new FileInputStream(f2);
            fInput22 = new FileInputStream(f2);
            fInput22.read(buf2);
            fInput2.skip(44);
            String header1 = getHeader(buf1, f1);
            String header2 = getHeader(buf2, f2);
            String[] firstValues = header1.split(",");
            String[] secondValues = header2.split(",");
            int sample_rate1 = Integer.parseInt(firstValues[3]);
            int sample_rate2 = Integer.parseInt(secondValues[3]);
            List<Double> smMags;
            List<Double> lfMags;
            smMags = checkMag(size1, fInput1);
            lfMags = checkMag(size2, fInput2);
            double rmsSmall;
            double rmsLarge;
            if (smMags.size() == lfMags.size()) {
                if (computedWholeFileRMS.containsKey(f1.getName())){
                    rmsSmall = computedWholeFileRMS.get(f1.getName());
                }
                else {
                    rmsSmall = calRMS(smMags);
                    computedWholeFileRMS.put(f1.getName(), rmsSmall);
                }
                if (computedWholeFileRMS.containsKey(f2.getName())){
                    rmsLarge = computedWholeFileRMS.get(f2.getName());
                }
                else {
                    rmsLarge = calRMS(lfMags);
                    computedWholeFileRMS.put(f2.getName(), rmsLarge);
                }
                if (Math.abs(rmsSmall - rmsLarge) < 1.0) {
                    System.out.println("MATCH: 0.0 " + f1.getName() + " "
                            + f2.getName());
                    return;
                } else {
                    largerFileName = f1.getName();
                    smallerFileName = f2.getName();
                    if (songs.containsKey(largerFileName)) {
                        largerFileRMS = songs.get(largerFileName);
                    } else {
                        largerFileRMS = newRMS(smMags, sample_rate1);
                        songs.put(largerFileName, largerFileRMS);
                    }
                    if (songs.containsKey(smallerFileName)) {
                        smallerFileRMS = songs.get(smallerFileName);
                    } else {
                        smallerFileRMS = newRMS(lfMags, sample_rate2);

                        songs.put(smallerFileName, smallerFileRMS);
                    }
                }
            } else if (smMags.size() > lfMags.size()) {
                largerFileName = f1.getName();
                smallerFileName = f2.getName();
                if (songs.containsKey(largerFileName)) {
                    largerFileRMS = songs.get(largerFileName);
                } else {
                    largerFileRMS = newRMS(smMags, sample_rate1);
                    songs.put(largerFileName, largerFileRMS);
                }
                if (songs.containsKey(smallerFileName)) {
                    smallerFileRMS = songs.get(smallerFileName);
                } else {
                    smallerFileRMS = newRMS(lfMags, sample_rate2);

                    songs.put(smallerFileName, smallerFileRMS);
                }

            } else {
                largerFileName = f2.getName();
                smallerFileName = f1.getName();
                if (songs.containsKey(largerFileName)) {
                    largerFileRMS = songs.get(largerFileName);
                } else {
                    largerFileRMS = newRMS(lfMags, sample_rate2);
                    songs.put(largerFileName, largerFileRMS);
                }
                if (songs.containsKey(smallerFileName)) {
                    smallerFileRMS = songs.get(smallerFileName);
                } else {
                    smallerFileRMS = newRMS(smMags, sample_rate1);

                    songs.put(smallerFileName, smallerFileRMS);
                }
            }
            String myKey = largerFileName+"|"+smallerFileName;
            String myKey1 = smallerFileName+"|"+largerFileName;
            if (computedMatches.containsKey(myKey)){
                System.out.println("MATCH: "+ computedMatches.get(myKey) + " " + largerFileName + " " + smallerFileName);
                return;
            }
            if (computedMatches.containsKey(myKey1)){
                System.out.println("MATCH: "+ computedMatches.get(myKey1) +  " " + smallerFileName + " " + largerFileName);
                return;
            }
            if ((computedMismatches.containsKey(myKey))||(computedMismatches.containsKey(myKey1))){
                //System.out.println("Already MisMatch Computed");
                return;
            }

            checkMatch(smallerFileRMS, largerFileRMS, largerFileName,
                    smallerFileName);
        } catch (FileNotFoundException e) {
            System.err.println("ERROR: Given file does not exist.");
        } catch (IOException e) {
            System.err.println("ERROR: I/O Exception Occured.");
        }
    }

    /*
     * checking rms values calculated from magnitudes for each file and
     * comparing values against each other. A sequence of samples is considered
     * before we say they match.
     */
    private static boolean checkMatch(List<Boolean> rmsSmall,
            List<Boolean> rmsLarge, String largerFileName,
            String smallerFileName) {
        int x = 0;
        int count1 = 0;
        int threshold = (int)(rmsSmall.size()/20 + 2);
        Boolean isMatch = false;
        double preLarge = 0.0;
        double preSmall = 0.0;
        int exceptions = 0;
        for (int i = 0; i < rmsLarge.size(); i++) {
            for (int j = x; j < rmsSmall.size();) {
                if ((i == (rmsLarge.size() - 1)) && (j < (rmsSmall.size() - 1))) {
                    isMatch = false;
                    break;
                }
                if ((!(rmsLarge.get(i) ^ rmsSmall.get(j))) || (exceptions < threshold)) {
                    if (rmsLarge.get(i) ^ rmsSmall.get(j)) {
                        exceptions++;
                    }
                    isMatch = true;
                    x++;
                    break;
                } else {
                    count1++;
                    if (isMatch) {
                        i = i - x;
                        x = 0;
                    }
                    isMatch = false;
                    exceptions = 0;
                    break;
                }
            }
        }
        if (isMatch) {
            System.out.println("MATCH: " + (float) count1 / (float) 10 + " "
                    + largerFileName + " " + smallerFileName);
            String myKey = largerFileName +"|"+smallerFileName;
            Float seconds = (float) count1 / (float) 10;
            computedMatches.put(myKey, seconds);
        }
        else{
            String myKey = largerFileName +"|"+smallerFileName;
            Float seconds = (float)0;
            computedMismatches.put(myKey, seconds);
        }
        return isMatch;
    }

    /*
     * parsing through the header of the WAV file to retrieve values such as
     * sample rate, channels and frame size for further usage in determining the
     * channel information
     */
    private static String getHeader(byte[] buf, File f) {
        String header = "";
        javax.sound.sampled.AudioInputStream inS = null;
        AudioFormat aformat;
        try {
            inS = javax.sound.sampled.AudioSystem.getAudioInputStream(f);
            aformat = inS.getFormat();
            int sample_rate = (int) aformat.getSampleRate();
            int num_channels = (int) aformat.getChannels();
            int bytes_per_frame = (int) aformat.getFrameSize();
            int bits_per_sample = (int) aformat.getSampleSizeInBits();
            header = "" + num_channels + "," + bits_per_sample + ","
                    + bytes_per_frame + "," + sample_rate;
        } catch (IOException e) {
            System.err.println("ERROR: Unable to parse the WAV file");
        } catch (UnsupportedAudioFileException e) {
            System.err.println("ERROR: Unable to parse the WAV file");
        }
        return header;
    }

    /*
     * Used to calculate RMS values, when files are equal in size. When we have
     * files which are equal in size, we are calculating RMS for the entire file
     * at once and comparing those values.
     */
    private static double calRMS(List<Double> mgar) {
        double sqr;
        double rms = 0;
        for (int i = 0; i < mgar.size(); i++) {
            sqr = Math.pow(mgar.get(i), 2);
            rms = rms + sqr;
        }
        double rms_final = Math.sqrt(rms / mgar.size());
        return rms_final;
    }

    /*
     * Calculating Root Mean Error(RME) for the list of magnitudes/frequencies
     * using the sample rate of the file. Root mean squared error values are
     * calculated for 1/10th of a second. RMS value for each 1/10th of a second
     * is compared against the previous second and a boolean array is formed is
     * such a fashion: if previousrmsval <= currentrmsval add true to the rms
     * list else add false to the rms list
     * 
     * Rather than comparing the actual RMS values, we will the using the
     * boolean output array which represents a sound wave.
     * 
     * Example output of this function: file1 : tftttftttftttffff file2 :
     * tftftftffftttffft
     */
    private static List<Boolean> newRMS(List<Double> mgar, int sample_rate) {
        // int count = 0;
        int s_rate = sample_rate / 10;
        List<Boolean> rmsList = new ArrayList<Boolean>();
        double rms = 0.0;
        double previousRms = 0.0;
        for (int k = 0; k < ((mgar.size() - 1) + s_rate); k += (s_rate)) {
            List<Double> ls = new ArrayList<Double>();
            if ((k + (s_rate)) < mgar.size()) {
                ls = mgar.subList(k, k + (s_rate));
            } else {
                break;
            }
            rms = calRMS(ls);
            if (previousRms <= rms) {
                rmsList.add(true);
            } else {
                rmsList.add(false);
            }
            previousRms = rms;
            // count++;
        }
        return rmsList;
    }

    /*
     * Creating complex numbers from byte data of the wav or mp3 files and
     * passing the complex numbers to FFT to change from time domain to
     * frequency domain and then calculating the magnitudes for the complex
     * numbers returned by FFt
     * 
     * Given a FileInputStream and length of the file, converts the given audio
     * file into a frequency domain signal and returns list of
     * magnitudes(frequencies). We have used a version of FFT available online.
     * FFT returns a list of complex numbers when we process an audio file.
     */
    private static List<Double> checkMag(long len, FileInputStream fis) {
        int CSIZE = 2;
        int index = 0;
        byte[] buf = new byte[(int) len];
        List<Double> mags = new ArrayList<Double>();
        try {
            fis.read(buf);
        } catch (IOException e) {
            System.err.println("ERROR: Cannot read the given input file");
        }
        int totalSize = buf.length;
        int amount = totalSize / CSIZE;
        Complex[][] results = new Complex[1][1];
        double magnitude;
        for (int times = 0, j = 0; times < amount; times++, j += 2) {
            Complex[] complex = new Complex[1];
            magnitude = 0;
            complex[index] = new Complex(buf[(j + 1)], index);
            FFT.doFFT(complex);
            results[0] = FFT.getC();
            magnitude = Math
                    .sqrt((results[index][index].re()
                            * results[index][index].re() + (results[index][index]
                            .im() * results[index][index].im())));
            mags.add(magnitude);
        }
        return mags;
    }

    /*
     * Given a file, returns true if the file is a WAV file, else returns false.
     * We used the UNIX file utility to check for the WAV format
     */
    private static boolean checkWave(File f) {
        boolean isWave = false;
        Process p1;
        Runtime rt = Runtime.getRuntime();
        try {
            p1 = rt.exec(new String[] { "file", f.getAbsolutePath() });
            try {
                int pexit = p1.waitFor();
            } catch (InterruptedException e) {
                System.err.println("ERROR: ProcessBuilder execution error.");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    p1.getInputStream()));
            String line = reader.readLine();
            reader.close();
            if (line.contains("RIFF")) {
                isWave = true;
            }
            p1.destroy();
        } catch (IOException e) {
            System.err.println("ERROR: ProcessBuilder execution error.");
        }
        return isWave;
    }

    /*
     * Given a file, returns true if the file is an MP3, else returns false. We
     * used the UNIX File utility to check for the MP3 format
     */
    private static boolean checkMP3(File f) {
        boolean ismp3 = false;
        Process p1;
        Runtime rt = Runtime.getRuntime();
        try {
            p1 = rt.exec(new String[] { "file", f.getAbsolutePath() });
            try {
                int pexit = p1.waitFor();
            } catch (InterruptedException e) {
                System.err.println("ERROR: MP3 format checking failed");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    p1.getInputStream()));
            String line = reader.readLine();
            reader.close();
            if (line.contains("MPEG ADTS, layer III")) {
                ismp3 = true;
            }
            p1.destroy();
        } catch (IOException e) {
            System.err.println("ERROR: MP3 format checking failed");
        }

        return ismp3;
    }

    /*
     * Given an MP3 file, converts the file to a mono WAV file and returns the
     * same
     * 
     * We used the LAME utility available at /course/cs5500f13/bin directory
     * with the --decode option to convert MP3 files to WAV
     */
    private static File convertMP3(File file) {
        File dest_file = new File("temp/" + file.getName());
        if (dest_file.isFile() && checkWave(file)) {
            return dest_file;
        } else {
            try {
                ProcessBuilder pb = new ProcessBuilder("/course/cs5500f13/bin/lame",
                        "--decode", "-a", "--mp3input", 
                        file.getAbsolutePath(),
                        "temp/" + file.getName());
                pb.redirectErrorStream(true);
                Process p = pb.start();
                String line = null;
                InputStreamReader isr = new InputStreamReader(
                        p.getInputStream());
                BufferedReader br = new BufferedReader(isr);
                while ((line = br.readLine()) != null) {
                }
                int rc = p.waitFor();
                dest_file = new File("temp/" + file.getName());
            } catch (IOException e) {
                System.err.println("ERROR: MP3 to WAV conversion error");
            } catch (InterruptedException ie) {
                System.err.println("ERROR: MP3 to WAV conversion error");
            }
        }

        return dest_file;
    }

    /*
     * Given an WAV file, converts the file to a stereo MP3 and returns the same
     * 
     * We used the LAME utility available at /course/cs5500f13/bin directory
     * with the -m m option to convert to a mono MP3 file. This function is used
     * to convert a stereo WAV file to Mono WAV file. Since we can't directly
     * convert a stereo WAV to a Mono WAV, we are converting the WAV to MP3 and
     * converting MP3 back to WAV.
     */
    private static File convertWAV(File file) {
        File dest_file = new File("wvtmp/" + file.getName());
        if (dest_file.isFile() && checkWave(file)) {
            return dest_file;
        } else {
            try {
                ProcessBuilder pb = new ProcessBuilder("/course/cs5500f13/bin/lame", 
                        file.getAbsolutePath(), "wvtmp/"
                                + file.getName());
                pb.redirectErrorStream(true);
                Process p = pb.start();
                String line = null;
                InputStreamReader isr = new InputStreamReader(
                        p.getInputStream());
                BufferedReader br = new BufferedReader(isr);
                while ((line = br.readLine()) != null) {
                }
                int rc = p.waitFor();
                dest_file = new File("wvtmp/" + file.getName());
            } catch (IOException e) {
                System.err.println("ERROR: WAV to MP3 conversion error");
            } catch (InterruptedException ie) {
                System.err.println("ERROR: WAV to MP3 conversion error");
            }
        }

        return dest_file;
    }

    /*
     * Given a stereo MP3 file, converts it to a Mono MP3 file and returns the
     * same
     * 
     * We used the LAME utility available at /course/cs5500f13/bin directory
     * with the -a option to convert to a mono MP3 file.
     */
    private static File convertMP3Mono(File file) {
        File dest_file = new File("mp3tmp/" + file.getName());
        if (dest_file.isFile() && checkWave(file)) {
            return dest_file;
        } else {
            try {
                ProcessBuilder pb = new ProcessBuilder("/course/cs5500f13/bin/lame",
                        "-V2", "-a", "--mp3input", 
                        file.getAbsolutePath(),
                        "mp3tmp/" + file.getName());
                pb.redirectErrorStream(true);
                Process p = pb.start();
                String line = null;
                InputStreamReader isr = new InputStreamReader(
                        p.getInputStream());
                BufferedReader br = new BufferedReader(isr);
                while ((line = br.readLine()) != null) {
                }
                int rc = p.waitFor();
                dest_file = new File("mp3tmp/" + file.getName());
            } catch (IOException e) {
                System.err.println("ERROR: MP3 to WAV conversion error");
            } catch (InterruptedException ie) {
                System.err.println("ERROR: MP3 to WAV conversion error");
            }
        }

        return dest_file;
    }
}
