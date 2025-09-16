USE [GAZ]
GO

/****** Object:  Table [dbo].[demployes]    Script Date: 16.09.2025 9:12:35 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[demployes](
                                  [ID] [int] IDENTITY(1,1) NOT NULL,
                                  [EMPLOYEEID] [varchar](45) NULL,
                                  [LASTNAMERUS] [varchar](45) NULL,
                                  [NAMERUS] [varchar](45) NULL,
                                  [MIDDLENAMERUS] [varchar](45) NULL,
                                  [TABNOM] [varchar](45) NULL,
                                  [JOBTITLERUS] [varchar](255) NULL,
                                  [email] [varchar](45) NULL,
                                  [IPPHONE] [varchar](45) NULL,
                                  [WORKPHONE] [varchar](45) NULL,
                                  [TYPE_WORK] [varchar](45) NULL,
                                  [DEPARTMENTID] [varchar](45) NULL,
                                  [MANAGERID] [varchar](45) NULL,
                                  [LOGINNAME] [varchar](45) NULL,
                                  [USER_SID] [varchar](50) NULL,
                                  [date_create] [varchar](45) NULL,
                                  PRIMARY KEY CLUSTERED
                                      (
                                       [ID] ASC
                                          )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [SYSTEM],
                                  UNIQUE NONCLUSTERED
                                      (
                                       [ID] ASC
                                          )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [SYSTEM]
) ON [SYSTEM]
GO
CREATE NONCLUSTERED INDEX [NonClusteredIndex-20250916-091251] ON [dbo].[demployes]
    (
     [EMPLOYEEID] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [SYSTEM]
GO