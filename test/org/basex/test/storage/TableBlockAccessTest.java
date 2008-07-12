package org.basex.test.storage;

import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.basex.build.DiskBuilder;
import org.basex.build.xml.XMLParser;
import org.basex.core.proc.Drop;
import org.basex.core.proc.Open;
import org.basex.io.IO;
import org.basex.io.TableDiskAccess;
import static org.junit.Assert.*;
import static org.basex.data.DataText.*;

/**
 * This class tests the update functionality of the BlockStorage.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-07, ISC License
 * @author Tim Petrowsky
 */
public final class TableBlockAccessTest {
  /** Test file size. */
  private int size;
  /** BlockStorage. */
  private TableDiskAccess tba;
  /** Test file we do updates with. */
  private static final String TESTFILE = "etc/xml/JUnit.xml";
  /** Test database name. */
  private static final String DBNAME = "JUnitTest";
  /** Starting storage. */
  private byte[] storage;
  /** Max entries per block. */
  private int maxEntriesPerBlock;
  /** Expected blocks in file. */
  private int blocks;
  /** Nodes per block. */
  private int nodesPerBlock;

  /**
   * Delete old JUnitTest databases.
   */
  @BeforeClass
  public static void setUpBeforeClass() {
    Drop.drop(DBNAME);
  }

  /**
   * Load the JUnitTest database.
   * @throws IOException in case of probs
   */
  @Before
  public void setUp() throws IOException {
    final IO file = new IO(TESTFILE);
    new DiskBuilder().build(new XMLParser(file), DBNAME);
    size = Open.open(DBNAME).size;
    tba = new TableDiskAccess(DBNAME, DATATBL);
    final int bytecount = size * (1 << IO.NODEPOWER);
    storage = new byte[bytecount];
    for(int i = 0; i < bytecount; i++) {
      storage[i] = (byte) tba.read1(i >> IO.NODEPOWER, i % (1 << IO.NODEPOWER));
    }
    maxEntriesPerBlock =
      (1 << IO.BLOCKPOWER) >>> IO.NODEPOWER;
    blocks = (int) Math.ceil(size /
        Math.floor(maxEntriesPerBlock * IO.BLOCKFILL));
    nodesPerBlock = (int) (maxEntriesPerBlock * IO.BLOCKFILL);
  }

  /**
   * Close and reload storage.
   */
  private void closeAndReload() {
    try {
      tba.close();
      tba = null;
      tba = new TableDiskAccess(DBNAME, DATATBL);
    } catch(final IOException e) {
      fail();
    }
  }

  /**
   * Compare old entrys with new entries.
   * @param startNodeNumber first old entry to compare
   * @param currentNodeNumber first new entry to compare
   * @param count number of entrys to compare
   */
  private void assertEntrysEqual(final int startNodeNumber,
      final int currentNodeNumber, final int count) {
    final int startOffset = startNodeNumber << IO.NODEPOWER;
    final int currentOffset = currentNodeNumber << IO.NODEPOWER;
    for(int i = 0; i < (count << IO.NODEPOWER); i++) {
      final int startByteNum = startOffset + i;
      final int currentByteNum = currentOffset + i;
      final byte startByte = storage[startByteNum];
      final byte currentByte = (byte) tba.read1(currentByteNum >> IO.NODEPOWER,
        currentByteNum % (1 << IO.NODEPOWER));
      assertEquals("Old entry " + (startByteNum >> IO.NODEPOWER)
          + " (byte " + (startByteNum % (1 << IO.NODEPOWER))
          + ") and new entry " + (currentByteNum >> IO.NODEPOWER)
          + " (byte " + (currentByteNum % (1 << IO.NODEPOWER)) + ")",
          startByte, currentByte);
    }
  }

  /**
   * Dump storage.
   *
  private void dumpStorage() {
    for(int i = 0; i < (tba.getSize() << IOConstants.NODEPOWER); i++) {
      byte currentByte = (byte) tba.read1(i >> IOConstants.NODEPOWER, i
          % (1 << IOConstants.NODEPOWER));
      if((i % (1 << IOConstants.NODEPOWER)) == 0) System.out.printf(
          "\nEntry %3d : ", i >> IOConstants.NODEPOWER);
      System.out.printf("%02x ", currentByte);
    }
    System.out.println();
    System.out.println();
  }
   */

  /**
   * Test size of file.
   */
  @Test
  public void testSize() {
    assertEquals("Testfile size changed!", size, tba.size());
    assertTrue("Need at least 3 blocks for testing!", blocks > 2);
    assertEquals("Unexpected number of blocks!", blocks, tba.blocks());
    closeAndReload();
    assertEquals("Testfile size changed!", size, tba.size());
    assertTrue("Need at least 3 blocks for testing!", blocks > 2);
    assertEquals("Unexpected number of blocks!", blocks, tba.blocks());
  }

  /**
   * Test delete.
   */
  @Test
  public void testDeleteOneNode() {
    tba.delete(3, 1);
    assertEquals("One node deleted => size-1", size - 1, tba.size());
    assertEntrysEqual(0, 0, 3);
    assertEntrysEqual(4, 3, size - 4);
    closeAndReload();
    assertEquals("One node deleted => size-1", size - 1, tba.size());
    assertEntrysEqual(0, 0, 3);
    assertEntrysEqual(4, 3, size - 4);
  }

  /**
   * Test delete at beginning.
   */
  @Test
  public void testDeleteAtBeginning() {
    tba.delete(0, 3);
    assertEquals("Three nodes deleted => size-3", size - 3, tba.size());
    assertEntrysEqual(3, 0, size - 3);
    closeAndReload();
    assertEquals("Three nodes deleted => size-3", size - 3, tba.size());
    assertEntrysEqual(3, 0, size - 3);
  }

  /**
   * Test delete at end.
   */
  @Test
  public void testDeleteAtEnd() {
    tba.delete(size - 3, 3);
    assertEquals("Three nodes deleted => size-3", size - 3, tba.size());
    assertEntrysEqual(0, 0, size - 3);
    closeAndReload();
    assertEquals("Three nodes deleted => size-3", size - 3, tba.size());
    assertEntrysEqual(0, 0, size - 3);
  }

  /**
   * Delete first block.
   */
  @Test
  public void testDeleteFirstBlock() {
    tba.delete(0, nodesPerBlock);
    assertEquals(blocks - 1, tba.blocks());
    assertEntrysEqual(nodesPerBlock, 0, size - nodesPerBlock);
    closeAndReload();
    assertEquals(blocks - 1, tba.blocks());
    assertEntrysEqual(nodesPerBlock, 0, size - nodesPerBlock);
  }

  /**
   * Delete the second block.
   */
  @Test
  public void testDeleteSecondBlock() {
    tba.delete(nodesPerBlock, nodesPerBlock);
    assertEquals(blocks - 1, tba.blocks());
    assertEntrysEqual(0, 0, nodesPerBlock);
    assertEntrysEqual(2 * nodesPerBlock, nodesPerBlock, size - 2
        * nodesPerBlock);
    closeAndReload();
    assertEquals(blocks - 1, tba.blocks());
    assertEntrysEqual(0, 0, nodesPerBlock);
    assertEntrysEqual(2 * nodesPerBlock, nodesPerBlock, size - 2
        * nodesPerBlock);
  }

  /**
   * Delete the last block.
   */
  @Test
  public void testDeleteLastBlock() {
    tba.delete((size / nodesPerBlock) * nodesPerBlock, size % nodesPerBlock);
    assertEquals(blocks - 1, tba.blocks());
    assertEntrysEqual(0, 0, nodesPerBlock - (size % nodesPerBlock));
    closeAndReload();
    assertEquals(blocks - 1, tba.blocks());
    assertEntrysEqual(0, 0, nodesPerBlock - (size % nodesPerBlock));
  }

  /**
   * Delete the second block with some surrounding nodes.
   */
  @Test
  public void testDeleteSecondBlockAndSurroundingNodes() {
    tba.delete(nodesPerBlock - 1, nodesPerBlock + 2);
    assertEquals(size - 2 - nodesPerBlock, tba.size());
    assertEquals(blocks - 1, tba.blocks());
    assertEntrysEqual(0, 0, nodesPerBlock - 1);
    assertEntrysEqual(2 * nodesPerBlock + 1, nodesPerBlock - 1, size - 2
        * nodesPerBlock - 1);
    closeAndReload();
    assertEquals(size - 2 - nodesPerBlock, tba.size());
    assertEquals(blocks - 1, tba.blocks());
    assertEntrysEqual(0, 0, nodesPerBlock - 1);
    assertEntrysEqual(2 * nodesPerBlock + 1, nodesPerBlock - 1, size - 2
        * nodesPerBlock - 1);
  }

  /**
   * Test basic insertion.
   */
  @Test
  public void testSimpleInsert() {
    tba.insert(3, getTestEntries(1));
    assertEquals(size + 1, tba.size());
    assertEntrysEqual(0, 0, 4);
    assertAreInserted(4, 1);
    assertEntrysEqual(4, 5, size - 4);
    closeAndReload();
    assertEquals(size + 1, tba.size());
    assertEntrysEqual(0, 0, 4);
    assertAreInserted(4, 1);
    assertEntrysEqual(4, 5, size - 4);
  }

  /**
   * Test inserting multiple entries.
   */
  @Test
  public void testInsertMultiple() {
    tba.insert(3, getTestEntries(3));
    assertEquals(size + 3, tba.size());
    assertEntrysEqual(0, 0, 4);
    assertAreInserted(4, 3);
    assertEntrysEqual(4, 7, size - 4);
    closeAndReload();
    assertEquals(size + 3, tba.size());
    assertEntrysEqual(0, 0, 4);
    assertAreInserted(4, 3);
    assertEntrysEqual(4, 7, size - 4);

  }

  /**
   * Test inserting multiple entries.
   */
  @Test
  public void testInsertMany() {
    tba.insert(3, getTestEntries(maxEntriesPerBlock - 1));
    assertEquals(size + maxEntriesPerBlock - 1, tba.size());
    assertEntrysEqual(0, 0, 4);
    assertAreInserted(4, maxEntriesPerBlock - 1);
    assertEntrysEqual(4, 4 + maxEntriesPerBlock - 1, size - 4);
    closeAndReload();
    assertEquals(size + maxEntriesPerBlock - 1, tba.size());
    assertEntrysEqual(0, 0, 4);
    assertAreInserted(4, maxEntriesPerBlock - 1);
    assertEntrysEqual(4, 4 + maxEntriesPerBlock - 1, size - 4);
  }

  /**
   * Test inserting multiple entries.
   */
  @Test
  public void testInsertAtBlockBoundary() {
    tba.insert(nodesPerBlock - 1, getTestEntries(nodesPerBlock));
    assertEquals(size + nodesPerBlock, tba.size());
    assertEquals(blocks + 1, tba.blocks());
    assertEntrysEqual(0, 0, nodesPerBlock);
    assertAreInserted(nodesPerBlock, nodesPerBlock);
    assertEntrysEqual(nodesPerBlock, 2 * nodesPerBlock, size - nodesPerBlock);
    closeAndReload();
    assertEquals(size + nodesPerBlock, tba.size());
    assertEquals(blocks + 1, tba.blocks());
    assertEntrysEqual(0, 0, nodesPerBlock);
    assertAreInserted(nodesPerBlock, nodesPerBlock);
    assertEntrysEqual(nodesPerBlock, 2 * nodesPerBlock, size - nodesPerBlock);
  }

  /**
   * Assert that the chosen entries are inserted by a test case.
   * @param startNum first entry
   * @param count number of entries
   */
  private void assertAreInserted(final int startNum, final int count) {
    for(int i = 0; i < count; i++)
      for(int j = 0; j < (1 << IO.NODEPOWER); j++)
        assertEquals(5, tba.read1(startNum + i, j));
  }

  /**
   * Create a test-byte array containing the specified number of entries. All
   * bytes are set to (byte)5.
   * @param entries number of Entries to create
   * @return byte array containing the number of entries (all bytes 5)
   */
  private byte[] getTestEntries(final int entries) {
    final byte[] result = new byte[entries << IO.NODEPOWER];
    for(int i = 0; i < result.length; i++) result[i] = (byte) 5;
    return result;
  }

  /**
   * Drop the JUnitTest database.
   * @throws Exception in case of problems.
   */
  @After
  public void tearDown() throws Exception {
    tba.close();
    Drop.drop(DBNAME);
  }
}

