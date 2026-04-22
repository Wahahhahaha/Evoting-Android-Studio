-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Apr 22, 2026 at 07:07 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `voting`
--

-- --------------------------------------------------------

--
-- Table structure for table `angkatan`
--

CREATE TABLE `angkatan` (
  `angkatanid` int(11) NOT NULL,
  `nama` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `angkatan`
--

INSERT INTO `angkatan` (`angkatanid`, `nama`) VALUES
(1, 'X'),
(2, 'XI'),
(3, 'XII');

-- --------------------------------------------------------

--
-- Table structure for table `candidate`
--

CREATE TABLE `candidate` (
  `candidateid` int(11) NOT NULL,
  `picture` varchar(255) NOT NULL,
  `vice_picture` varchar(255) DEFAULT NULL,
  `president_studentid` int(11) DEFAULT NULL,
  `vice_studentid` int(11) DEFAULT NULL,
  `studentid` int(11) NOT NULL,
  `vision` text NOT NULL,
  `mission` text NOT NULL,
  `periodid` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `candidate`
--

INSERT INTO `candidate` (`candidateid`, `picture`, `vice_picture`, `president_studentid`, `vice_studentid`, `studentid`, `vision`, `mission`, `periodid`) VALUES
(15, 'uploads/president_1776669656_71181e97.jpg', 'uploads/vice_1776669656_f060c306.jpg', 2, 3, 2, 'asdf', 'asdf', 6),
(16, 'uploads/president_1776669677_970674d4.jpg', 'uploads/vice_1776669677_9a14efc6.jpg', 4, 5, 4, 'asdf', 'asdf', 6),
(17, 'uploads/president_1776669695_eae89b13.jpg', 'uploads/vice_1776669695_90476f4e.jpg', 6, 7, 6, 'asdf', 'asdf', 6),
(18, 'uploads/president_1776670741_a6b1072e.jpg', 'uploads/vice_1776670741_dc9e31c4.jpg', 2, 3, 2, 'asdf', 'asdf', 7);

-- --------------------------------------------------------

--
-- Table structure for table `class`
--

CREATE TABLE `class` (
  `classid` int(11) NOT NULL,
  `classname` varchar(255) NOT NULL,
  `jurusanid` int(11) NOT NULL,
  `angkatanid` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `class`
--

INSERT INTO `class` (`classid`, `classname`, `jurusanid`, `angkatanid`) VALUES
(1, 'A', 1, 1);

-- --------------------------------------------------------

--
-- Table structure for table `jurusan`
--

CREATE TABLE `jurusan` (
  `jurusanid` int(11) NOT NULL,
  `nama` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `jurusan`
--

INSERT INTO `jurusan` (`jurusanid`, `nama`) VALUES
(1, 'AKL'),
(2, 'BDP'),
(3, 'RPL');

-- --------------------------------------------------------

--
-- Table structure for table `level`
--

CREATE TABLE `level` (
  `levelid` int(11) NOT NULL,
  `levelname` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `level`
--

INSERT INTO `level` (`levelid`, `levelname`) VALUES
(1, 'Admin'),
(2, 'Student');

-- --------------------------------------------------------

--
-- Table structure for table `student`
--

CREATE TABLE `student` (
  `studentid` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `phonenumber` varchar(255) NOT NULL,
  `classid` int(11) NOT NULL,
  `userid` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `student`
--

INSERT INTO `student` (`studentid`, `name`, `email`, `phonenumber`, `classid`, `userid`) VALUES
(2, 'Andi Saputra', 'andi@gmail.com', '084323134345', 1, 4),
(3, 'Budi Santoso', 'budi@gmail.com', '085623123081', 1, 5),
(4, 'Citra Lestari', 'citra@gmail.com', '081234567001', 1, 6),
(5, 'Dedi Pratama', 'dedi@gmail.com', '081234567002', 2, 7),
(6, 'Eka Putri', 'eka@gmail.com', '081234567003', 3, 8),
(7, 'Fajar Nugroho', 'fajar@gmail.com', '081234567004', 4, 9),
(8, 'Gina Maharani', 'gina@gmail.com', '081234567005', 5, 10),
(9, 'Hadi Saputra', 'hadi@gmail.com', '081234567006', 6, 11),
(10, 'Indah Sari', 'indah@gmail.com', '081234567007', 7, 12),
(11, 'Joko Susilo', 'joko@gmail.com', '081234567008', 8, 13),
(12, 'Kiki Amelia', 'kiki@gmail.com', '081234567009', 9, 14),
(13, 'Lukman Hakim', 'lukman@gmail.com', '081234567010', 1, 15);

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `userid` int(11) NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `levelid` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`userid`, `username`, `password`, `levelid`) VALUES
(1, 'admin', '$2y$12$O9wso5ya598BsysOG51izu/VqO0rWAsufnCm1cU3WBHmD322yn5Wy', 1),
(4, 'a', '$2y$12$u0zu1V4qJzAe0BcGeKrF8ufpGHTYWv6s6XVoLKtuCpoI2Iybh9JAS', 2),
(5, 'b', '$2y$12$c1UPECbR2tJBwVFeKQz9NODpnjvKpT2ERUULjl/PbLL6sB8KRgj4m', 2),
(6, 'c', '$2y$12$7dchPOTiKzV0.seA7/d7GOXuPfsbgLRXLmWs/87lVWRXZidd3NJBa', 2),
(7, 'd', '$2y$12$kL/EBRgnwS6CIMFGMKz3/u6uuBV53LzA.qQvNewbCcRiIOcTm1rB6', 2),
(8, 'e', '$2y$12$64/pznzYAKuEBCVp83vKWemDiQrySpQAlzJ8qYf/YYjcy0O/uKQXC', 2),
(9, 'f', '$2y$12$ylSDBH51u7KnIXVF2GWLyexRSHlICp87CVG.aCGCuC69LsAlp28q.', 2),
(10, 'g', '$2y$12$.cQ42gY054iNE9HU.UiIw.sYcFmqGTCJVBbmCAHA7qDfBcGT1iEZq', 2),
(11, 'h', '$2y$12$f0nCKyt6Vo/e/Rt2SJ28XeZmaO2Rn3vSkS9oCWK5nPY6Cx9B6vj0y', 2),
(12, 'i', '$2y$12$m1/m4DE6XwyOfP9zTbsEquoSle2Hju70v57NRHtMwjqvmroxvvkLG', 2),
(13, 'j', '$2y$12$w0CGJrLzc7y4AbhgwMG6PeC0SoC0iwdfdcEIdnovzB3fSBlYFYO4O', 2),
(14, 'k', '$2y$12$dvMb4esQWXgiuZLEdkEVDun7/jQNzFpAKOLWngoE7kQKbYTJnwyqy', 2),
(15, 'l', '$2y$12$4qKTUetwjsvsc/VZx5Bv4uGLx3dzI5xOnqL5q0VmfqEmtXsPvDFcm', 2);

-- --------------------------------------------------------

--
-- Table structure for table `vote`
--

CREATE TABLE `vote` (
  `voteid` int(11) NOT NULL,
  `studentid` int(11) NOT NULL,
  `candidateid` int(11) NOT NULL,
  `periodid` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `vote`
--

INSERT INTO `vote` (`voteid`, `studentid`, `candidateid`, `periodid`) VALUES
(3, 2, 17, 6),
(4, 3, 17, 6),
(5, 4, 15, 6),
(6, 5, 16, 6),
(7, 6, 15, 6),
(8, 7, 15, 6),
(9, 8, 15, 6),
(10, 9, 15, 6),
(11, 10, 15, 6),
(12, 11, 17, 6),
(13, 12, 15, 6),
(14, 13, 16, 6);

-- --------------------------------------------------------

--
-- Table structure for table `voting_period`
--

CREATE TABLE `voting_period` (
  `periodid` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `startdate` date NOT NULL,
  `enddate` date NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `voting_period`
--

INSERT INTO `voting_period` (`periodid`, `title`, `startdate`, `enddate`) VALUES
(6, '2024/2025', '2024-04-13', '2024-04-19'),
(7, '2025/2026', '2026-04-13', '2026-04-21');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `angkatan`
--
ALTER TABLE `angkatan`
  ADD PRIMARY KEY (`angkatanid`);

--
-- Indexes for table `candidate`
--
ALTER TABLE `candidate`
  ADD PRIMARY KEY (`candidateid`),
  ADD UNIQUE KEY `uniq_candidate_student_period` (`studentid`,`periodid`);

--
-- Indexes for table `class`
--
ALTER TABLE `class`
  ADD PRIMARY KEY (`classid`);

--
-- Indexes for table `jurusan`
--
ALTER TABLE `jurusan`
  ADD PRIMARY KEY (`jurusanid`);

--
-- Indexes for table `level`
--
ALTER TABLE `level`
  ADD PRIMARY KEY (`levelid`);

--
-- Indexes for table `student`
--
ALTER TABLE `student`
  ADD PRIMARY KEY (`studentid`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`userid`);

--
-- Indexes for table `vote`
--
ALTER TABLE `vote`
  ADD PRIMARY KEY (`voteid`),
  ADD UNIQUE KEY `studentid` (`studentid`,`periodid`),
  ADD UNIQUE KEY `uniq_vote_student_period` (`studentid`,`periodid`);

--
-- Indexes for table `voting_period`
--
ALTER TABLE `voting_period`
  ADD PRIMARY KEY (`periodid`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `angkatan`
--
ALTER TABLE `angkatan`
  MODIFY `angkatanid` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `candidate`
--
ALTER TABLE `candidate`
  MODIFY `candidateid` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=19;

--
-- AUTO_INCREMENT for table `class`
--
ALTER TABLE `class`
  MODIFY `classid` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT for table `jurusan`
--
ALTER TABLE `jurusan`
  MODIFY `jurusanid` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `level`
--
ALTER TABLE `level`
  MODIFY `levelid` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `student`
--
ALTER TABLE `student`
  MODIFY `studentid` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `userid` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT for table `vote`
--
ALTER TABLE `vote`
  MODIFY `voteid` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=15;

--
-- AUTO_INCREMENT for table `voting_period`
--
ALTER TABLE `voting_period`
  MODIFY `periodid` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
