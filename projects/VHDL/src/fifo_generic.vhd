-------------------------------------------------------------------------------
-- Title      : Generic FIFO
-- Project    : ORCC
-------------------------------------------------------------------------------
-- File       : generic_fifo.vhd
-- Author     : Nicolas Siret (nicolas.siret@ltdsa.com)
-- Company    : Lead Tech Design
-- Created    : 
-- Last update: 2010-04-20
-- Platform   : 
-- Standard   : VHDL'93
-------------------------------------------------------------------------------
-- Copyright (c) 2009-2010, LEAD TECH DESIGN Rennes - France
-- Copyright (c) 2009-2010, IETR/INSA of Rennes
-- All rights reserved.
-- 
-- Redistribution and use in source and binary forms, with or without
-- modification, are permitted provided that the following conditions are met:
-- 
--  -- Redistributions of source code must retain the above copyright notice,
--     this list of conditions and the following disclaimer.
--  -- Redistributions in binary form must reproduce the above copyright notice,
--     this list of conditions and the following disclaimer in the documentation
--     and/or other materials provided with the distribution.
--  -- Neither the name of the IETR/INSA of Rennes nor the names of its
--     contributors may be used to endorse or promote products derived from this
--     software without specific prior written permission.
-- 
-- THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
-- AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
-- IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
-- ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
-- LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
-- CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
-- SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
-- INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
-- STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
-- WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
-- SUCH DAMAGE.
-------------------------------------------------------------------------------
-- Revisions  :
-- Date        Version  Author  Description
-- 2010-02-09  1.0      LTD      Created
-------------------------------------------------------------------------------

library ieee;
use ieee.std_logic_1164.all;

-------------------------------------------------------------------------------


entity FIFO_generic is
  generic
    (
      depth : integer := 32;
      width : integer := 32);
  port
    (
      reset_n  : in  std_logic;
      --
      wr_clk   : in  std_logic;
      wr_data  : in  std_logic;
      data_in  : in  std_logic_vector (width -1 downto 0);
      full     : out std_logic;
      --
      rd_clk   : in  std_logic;
      rd_ack   : in  std_logic;
      send     : out std_logic;
      data_out : out std_logic_vector (width -1 downto 0);
      empty    : out std_logic);
end FIFO_generic;


-------------------------------------------------------------------------------


architecture arch_FIFO_generic of FIFO_generic is


  -----------------------------------------------------------------------------
  -- Internal type declarations
  -----------------------------------------------------------------------------

  type memory_type is array (depth -1 downto 0) of
    std_logic_vector(width -1 downto 0);
  signal memory : memory_type;

  -----------------------------------------------------------------------------
  -- Internal signal declarations
  -----------------------------------------------------------------------------
  --
  signal adresse_RAM_w         : integer range (depth -1) downto 0 := 0;
  signal adresse_RAM_r         : integer range (depth -1) downto 0 := 0;
  signal full_FIFO, empty_FIFO : std_logic;
  --
  -----------------------------------------------------------------------------
  
begin

  send     <= not empty_FIFO;
  empty    <= empty_FIFO;
  full     <= full_FIFO;
  data_out <= memory(adresse_RAM_r);

  -- purpose: store data
  RAM_write : process (wr_clk, reset_n)
  begin
    if reset_n = '0' then
      adresse_RAM_w <= 0;
      --
    elsif rising_edge(wr_clk) then
      if (wr_data = '1' and adresse_RAM_w < depth -1) then
        memory(adresse_RAM_w) <= data_in;
        adresse_RAM_w         <= adresse_RAM_w + 1;
      elsif (wr_data = '1') then
        memory(adresse_RAM_w) <= data_in;
        adresse_RAM_w         <= 0;
      end if;
    end if;
  end process RAM_write;


-- purpose: load data
  RAM_read : process (rd_clk, reset_n)
  begin
    if reset_n = '0' then
      adresse_RAM_r <= 0;
      --
    elsif rising_edge(rd_clk) then
      if (rd_ack = '1' and adresse_RAM_r < depth -1) then
        adresse_RAM_r <= adresse_RAM_r + 1;
      elsif (rd_ack = '1') then
        adresse_RAM_r <= 0;
      end if;
    end if;
  end process RAM_read;



-- Management of the flags
  Flag_proc : process (adresse_RAM_r, adresse_RAM_w) is
  begin
    if (adresse_RAM_w = adresse_RAM_r) then
      empty_FIFO <= '1';
      full_FIFO  <= '0';
      --
    elsif (adresse_RAM_w > adresse_RAM_r) then
      if (adresse_RAM_w - adresse_RAM_r < 3) then
        empty_FIFO <= '1';
        full_FIFO  <= '0';
      elsif (depth - adresse_RAM_w + adresse_RAM_r < 3) then
        empty_FIFO <= '0';
        full_FIFO  <= '1';
      else
        empty_FIFO <= '0';
        full_FIFO  <= '0';
      end if;
    else
      if (adresse_RAM_r - adresse_RAM_w < 3) then
        empty_FIFO <= '0';
        full_FIFO  <= '1';
      elsif (depth - adresse_RAM_r + adresse_RAM_w < 3) then
        empty_FIFO <= '1';
        full_FIFO  <= '0';
      else
        empty_FIFO <= '0';
        full_FIFO  <= '0';
      end if;
    end if;
  end process Flag_proc;

end arch_FIFO_generic;

--------------------------------------------------------------------------
