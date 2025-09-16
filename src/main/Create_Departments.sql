USE [GAZ]
GO

/****** Object:  Table [dbo].[dDepartments]    Script Date: 16.09.2025 8:26:56 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[dDepartments](
                                     [ID] [int] IDENTITY(1,1) NOT NULL,
                                     [DepartmentID] [varchar](45) NULL,
                                     [NAME] [varchar](255) NULL,
                                     [MANAGERID] [varchar](45) NULL,
                                     [MANAGERLOGINNAME] [varchar](45) NULL,
                                     [PARENTID] [varchar](45) NULL,
                                     [TYPE_NAME] [varchar](45) NULL,
                                     [CODE] [varchar](55) NULL,
                                     [B_DATE] [varchar](45) NULL,
                                     [E_DATE] [varchar](45) NULL,
                                     [DATA_INTEG] [varchar](45) NULL,
                                     [E_DOC] [varchar](45) NULL,
                                     [ID_DEPT_OWN] [varchar](45) NULL,
                                     [DATE_CREATE] [varchar](45) NULL,
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
CREATE NONCLUSTERED INDEX [NonClusteredIndex-20250916-082613] ON [dbo].[dDepartments]
    (
     [DepartmentID] ASC
        )WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [SYSTEM]
GO