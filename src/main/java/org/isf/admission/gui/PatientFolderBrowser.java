/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2021 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
 *
 * Open Hospital is a free and open source software for healthcare data management.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * https://www.gnu.org/licenses/gpl-3.0-standalone.html
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isf.admission.gui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventListener;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.EventListenerList;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.isf.admission.manager.AdmissionBrowserManager;
import org.isf.admission.model.Admission;
import org.isf.dicom.gui.DicomGui;
import org.isf.disease.manager.DiseaseBrowserManager;
import org.isf.disease.model.Disease;
import org.isf.examination.manager.ExaminationBrowserManager;
import org.isf.examination.model.PatientExamination;
import org.isf.generaldata.GeneralData;
import org.isf.generaldata.MessageBundle;
import org.isf.lab.manager.LabManager;
import org.isf.lab.model.Laboratory;
import org.isf.lab.service.LabIoOperations;
import org.isf.medstockmovtype.gui.MedicalsrMovPatList;
import org.isf.menu.gui.MainMenu;
import org.isf.menu.manager.Context;
import org.isf.opd.manager.OpdBrowserManager;
import org.isf.opd.model.Opd;
import org.isf.operation.gui.OperationList;
import org.isf.patient.gui.PatientInsert;
import org.isf.patient.gui.PatientInsertExtended;
import org.isf.patient.gui.PatientSummary;
import org.isf.patient.model.Patient;
import org.isf.stat.gui.report.GenericReportAdmission;
import org.isf.stat.gui.report.GenericReportDischarge;
import org.isf.stat.gui.report.GenericReportOpd;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.gui.OHServiceExceptionUtil;
import org.isf.utils.jobjects.MessageDialog;
import org.isf.utils.jobjects.ModalJFrame;
import org.isf.utils.jobjects.OhDefaultCellRenderer;
import org.isf.utils.table.TableSorter;
import org.isf.ward.manager.WardBrowserManager;
import org.isf.ward.model.Ward;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class shows patient data and the list of admissions and lab exams.
 *
 * last release  jun-14-08
 * @author chiara
 *
 * ----------------------------------------------------------
 * modification history
 * ====================
 * 14/06/08 - chiara - first version
 *                     
 * 30/06/08 - fabrizio - implemented automatic selection of exams within the admission period
 * 05/09/08 - alessandro - second version:
 * 						 - same PatientSummary than PatientDataBrowser
 * 						 - includes OPD in the table
 * -----------------------------------------------------------
 */
public class PatientFolderBrowser extends ModalJFrame implements 
				PatientInsert.PatientListener, PatientInsertExtended.PatientListener, AdmissionBrowser.AdmissionListener  {

	private static final long serialVersionUID = -3427327158197856822L;

	private static final Logger LOGGER = LoggerFactory.getLogger(PatientFolderBrowser.class);

	private EventListenerList deleteAdmissionListeners = new EventListenerList();

    public interface DeleteAdmissionListener extends EventListener {
        void deleteAdmissionUpdated(AWTEvent e);
    }

    public void addDeleteAdmissionListener(DeleteAdmissionListener l) {
        deleteAdmissionListeners.add(DeleteAdmissionListener.class, l);
    }

    public void removeDeleteAdmissionListener(DeleteAdmissionListener listener) {
        deleteAdmissionListeners.remove(DeleteAdmissionListener.class, listener);
    }
	
	//---------------------------------------------------------------------
	
	@Override
	public void patientInserted(AWTEvent e) {
	}

	@Override
	public void patientUpdated(AWTEvent e) {
		jContentPane = null;
		initialize();		
	}
	
	@Override
	public void admissionInserted(AWTEvent e) {
	}

	@Override
	public void admissionUpdated(AWTEvent e) {
		jContentPane = null;
		initialize();		
	}
	
	

	private Patient patient;
	
	public PatientFolderBrowser(AdmittedPatientBrowser listener,  Patient myPatient) {
		super();
		patient = myPatient;
		initialize();
	}

	private void initialize() {

		this.setContentPane(getJContentPane());
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setTitle(MessageBundle.getMessage("angal.admission.patientdata.title"));

		pack();
		setLocationRelativeTo(null);
		setResizable(false);
		setVisible(true);
	}

	private JPanel jContentPane = null;

	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getPatientDataPanel(), java.awt.BorderLayout.NORTH);
			jContentPane.add(getButtonPanel(), java.awt.BorderLayout.SOUTH);
		}
		return jContentPane;
	}
	
	private JPanel patientData = null;

	private JPanel getPatientDataPanel() {
		patientData = new JPanel();
		patientData.setLayout(new BorderLayout());
		patientData.add(getTablesPanel(), BorderLayout.EAST);

		PatientSummary ps = new PatientSummary(patient);
		JPanel pp = ps.getPatientCompleteSummary();
		patientData.add(pp, BorderLayout.WEST);

		return patientData;
	}

	private static final String DATE_FORMAT = "dd/MM/yy";

	private List<Admission> admList;
	private List<Laboratory> labList;
	private List<Disease> disease;
	private List<Ward> ward;
	private List<Opd> opdList;
	private List <PatientExamination> examinationList;
	
    private OperationList opeList;

	private String[] pColumns = {
			MessageBundle.getMessage("angal.common.date.txt").toUpperCase(),
			MessageBundle.getMessage("angal.common.ward.txt").toUpperCase(),
			MessageBundle.getMessage("angal.admission.diagnosisin.col").toUpperCase(),
			MessageBundle.getMessage("angal.admission.diagnosisout.col").toUpperCase(),
			MessageBundle.getMessage("angal.common.status.txt").toUpperCase()
	};
	private int[] pColumnWidth = {120, 150, 200, 200, 120};
	
	private String[] plColumns = {
			MessageBundle.getMessage("angal.common.date.txt").toUpperCase(),
			MessageBundle.getMessage("angal.common.exam.txt").toUpperCase(),
			MessageBundle.getMessage("angal.common.code.txt").toUpperCase(),
			MessageBundle.getMessage("angal.common.result.txt").toUpperCase()
	};
	private int[] plColumnwidth = {150, 200, 50, 200};

	private DefaultTableModel admModel;
	private DefaultTableModel labModel;
	private TableSorter sorter;
	private TableSorter sorterLab;
	private OhDefaultCellRenderer cellRenderer = new OhDefaultCellRenderer();

	private GregorianCalendar olderDate;

	private JTable admTable;
	private JTable labTable;

	private JScrollPane scrollPane;
	private JScrollPane scrollPaneLab;

	private JPanel tablesPanel=null;

	private MedicalsrMovPatList drugsList;

	private JPanel getTablesPanel() {
		tablesPanel = new JPanel(new BorderLayout());

		admModel = new AdmissionBrowserModel();
		sorter = new TableSorter(admModel);
		admTable = new JTable(sorter);

		/* ** apply default oh cellRender **** */
		admTable.setDefaultRenderer(Object.class, cellRenderer);
		admTable.setDefaultRenderer(Double.class, cellRenderer);
		admTable.addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent e) {
				JTable aTable = (JTable) e.getSource();
				int itsRow = aTable.rowAtPoint(e.getPoint());
				if (itsRow >= 0) {
					cellRenderer.setHoveredRow(itsRow);
				} else {
					cellRenderer.setHoveredRow(-1);
				}
				aTable.repaint();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
			}
		});
		admTable.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseExited(MouseEvent e) {
				cellRenderer.setHoveredRow(-1);
			}
		});

		for (int i = 0; i < pColumns.length; i++) {
			admTable.getColumnModel().getColumn(i).setPreferredWidth(pColumnWidth[i]);
			if (i == 0 || i == 4) {
				admTable.getColumnModel().getColumn(i).setCellRenderer(new DateCellRenderer());
			}
		}

		scrollPane = new JScrollPane(admTable);
		scrollPane.setPreferredSize(new Dimension(500, 200));
		tablesPanel.add(scrollPane, BorderLayout.NORTH);
		sorter.sortByColumn(0, false); //sort by first column, descending
		sorter.updateRowHeights(admTable);

		labModel = new LabBrowserModel();
		sorterLab = new TableSorter(labModel);
		labTable = new JTable(sorterLab);
		/* ** apply default oh cellRender **** */
		labTable.setDefaultRenderer(Object.class, cellRenderer);
		labTable.setDefaultRenderer(Double.class, cellRenderer);
		labTable.addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent e) {
				JTable aTable = (JTable) e.getSource();
				int itsRow = aTable.rowAtPoint(e.getPoint());
				if (itsRow >= 0) {
					cellRenderer.setHoveredRow(itsRow);
				} else {
					cellRenderer.setHoveredRow(-1);
				}
				aTable.repaint();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
			}
		});
		labTable.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseExited(MouseEvent e) {
				cellRenderer.setHoveredRow(-1);
			}
		});
		sorterLab.sortByColumn(0, false);

		for (int i = 0; i < plColumns.length; i++) {
			labTable.getColumnModel().getColumn(i).setPreferredWidth(plColumnwidth[i]);
			if (i == 0) {
				labTable.getColumnModel().getColumn(i).setCellRenderer(new DateCellRenderer());
			}
		}

        JTabbedPane tabbedPaneLabOpe = new JTabbedPane(SwingConstants.TOP);
		tablesPanel.add(tabbedPaneLabOpe, BorderLayout.CENTER);
		scrollPaneLab = new JScrollPane(labTable);
		tabbedPaneLabOpe.addTab(MessageBundle.getMessage("angal.admission.patientfolder.exams.title"), null, scrollPaneLab, null);

        opeList = new OperationList(patient);
        getOlderDate(opeList.getOprowData(), "opDate");
		tabbedPaneLabOpe.addTab(MessageBundle.getMessage("angal.admission.patientfolder.operations.title"), null, opeList, null);

		drugsList = new MedicalsrMovPatList(patient);
		getOlderDate(drugsList.getDrugsData(), "date");
		tabbedPaneLabOpe.addTab(MessageBundle.getMessage("angal.admission.patientfolder.drugs.title"), null, drugsList, null);

		ListSelectionModel listSelectionModel = admTable.getSelectionModel();
		listSelectionModel.addListSelectionListener(selectionEvent -> {

			// Check that mouse has been released.
			if (!selectionEvent.getValueIsAdjusting()) {
				GregorianCalendar startDate = null;
				GregorianCalendar endDate = null;
				int selectedRow = admTable.getSelectedRow();
				Object selectedObject = sorter.getValueAt(selectedRow, -1);
				Object selectedObject2;
				Admission adm2 = null;
				Opd opd2 = null;
				PatientExamination exam2 = null;

				// Get previous element in list
				if (selectedRow > 0) {
					 selectedObject2 = sorter.getValueAt(selectedRow - 1, -1);
					 if (selectedObject2 instanceof Admission) {
						 adm2 = (Admission) selectedObject2;
					 } else if (selectedObject2 instanceof Opd) {
						 opd2 = (Opd) selectedObject2;
					 } else if (selectedObject2 instanceof PatientExamination) {
						 exam2 = (PatientExamination) selectedObject2;
					 }
				}

				if (selectedObject instanceof Admission) {

					Admission ad = (Admission) selectedObject;
					startDate = ad.getAdmDate();
					endDate = ad.getDisDate();

				} else if (selectedObject instanceof Opd) {

					Opd opd = (Opd) selectedObject;
					startDate = opd.getVisitDate();

				} else if (selectedObject instanceof PatientExamination) {
					PatientExamination exam = (PatientExamination) selectedObject;
					startDate = exam.getPex_date();
				}

				if (opd2 != null) {
					endDate = opd2.getVisitDate();
				}
				if (adm2 != null) {
					endDate = adm2.getAdmDate();
				}
				if (exam2 != null) {
					endDate = exam2.getPex_date();
				}

				// Clear past selection, if any.
				opeList.selectCorrect(startDate, endDate);

				labTable.clearSelection();
				for (int i = 0; i < labList.size(); i++) {
					//Laboratory laboratory = labList.get(i);
					Laboratory laboratory = (Laboratory) sorterLab.getValueAt(i, -1);
					Date examDate = laboratory.getExamDate().getTime();

					// Check that the exam date is included between admission date and discharge date.
					// If the patient has not been discharged yet (and then discharge date doesn't exist)
					// check only that the exam date is the same or after the admission date.
					// On true condition select the corresponding table row.
					if (!examDate.before(startDate.getTime()) &&
							(null == endDate ? true : !examDate.after(endDate.getTime())))  {

						labTable.addRowSelectionInterval(i, i);

					}
				}
			}
		});

		return tablesPanel;
	}

	private JPanel getButtonPanel() {
		JPanel buttonPanel;
			buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,5,5));
			if (MainMenu.checkUserGrants("btnpatfoldopdrpt")) {
				buttonPanel.add(getOpdReportButton(), null);
			}
			if (MainMenu.checkUserGrants("btnpatfoldadmrpt")) {
				buttonPanel.add(getAdmReportButton(), null);
			}
			if (MainMenu.checkUserGrants("btnpatfoldadmrpt")) {
				buttonPanel.add(getDisReportButton(), null);
			}
			if (MainMenu.checkUserGrants("btnpatfoldpatrpt")) {
				buttonPanel.add(getLaunchReportButton(), null);
			}
            if (GeneralData.DICOMMODULEENABLED && MainMenu.checkUserGrants("btnpatfolddicom")) {
	            buttonPanel.add(getDICOMButton(), null);
            }
			buttonPanel.add(getCloseButton(), null);
		return buttonPanel;
	}

	private JButton opdReportButton = null;
	private JButton admReportButton = null;
	private JButton disReportButton = null;
	private JButton launchReportButton = null;
    private JButton dicomButton = null;
	private JButton closeButton=null;

	private JButton getOpdReportButton() {
		if (opdReportButton == null) {
			opdReportButton = new JButton(MessageBundle.getMessage("angal.admission.patientfolder.opdchart.btn"));
			opdReportButton.setMnemonic(MessageBundle.getMnemonic("angal.admission.patientfolder.opdchart.btn.key"));
			opdReportButton.addActionListener(actionEvent -> {
				if (admTable.getSelectedRow() < 0) {
					MessageDialog.error(null, "angal.common.pleaseselectarow.msg");
					return;
				}

				int selectedRow = admTable.getSelectedRow();
				Object selectedObj = sorter.getValueAt(selectedRow, -1);

				if (selectedObj instanceof Opd) {
					Opd opd = (Opd) sorter.getValueAt(selectedRow, -1);
					new GenericReportOpd(opd.getCode(), opd.getPatient().getCode(), GeneralData.OPDCHART);
				} else {
					MessageDialog.error(PatientFolderBrowser.this, "angal.admission.patientfolder.pleaseselectanopd.msg");
				}
			});
		}
		return opdReportButton;
	}

	private JButton getDisReportButton() {
		if (disReportButton == null) {
			disReportButton = new JButton(MessageBundle.getMessage("angal.admission.patientfolder.dischart.btn"));
			disReportButton.setMnemonic(MessageBundle.getMnemonic("angal.admission.patientfolder.dischart.btn.key"));
			disReportButton.addActionListener(actionEvent -> {
				if (admTable.getSelectedRow() < 0) {
					MessageDialog.error(null, "angal.common.pleaseselectarow.msg");
					return;
				}

				int selectedRow = admTable.getSelectedRow();
				Object selectedObj = sorter.getValueAt(selectedRow, -1);

				if (selectedObj instanceof Admission) {

					Admission adm = (Admission) sorter.getValueAt(selectedRow, -1);
					if (adm.getDisDate() == null) {
						MessageDialog.error(PatientFolderBrowser.this, "angal.admission.patientfolder.thepatientisnotyetdischarged.msg");
						return;
					}
					new GenericReportDischarge(adm.getId(), adm.getPatient().getCode(), GeneralData.DISCHART);
				} else {
					MessageDialog.error(PatientFolderBrowser.this, "angal.admission.patientfolder.pleaseselectanadmission.msg");
				}
			});
		}
		return disReportButton;
	}

	private JButton getAdmReportButton() {
		if (admReportButton == null) {
			admReportButton = new JButton(MessageBundle.getMessage("angal.admission.patientfolder.admchart.btn"));
			admReportButton.setMnemonic(MessageBundle.getMnemonic("angal.admission.patientfolder.admchart.btn.key"));
			admReportButton.addActionListener(actionEvent -> {
				if (admTable.getSelectedRow() < 0) {
					MessageDialog.error(null, "angal.common.pleaseselectarow.msg");
					return;
				}

				int selectedRow = admTable.getSelectedRow();
				Object selectedObj = sorter.getValueAt(selectedRow, -1);

				if (selectedObj instanceof Admission) {

					Admission adm = (Admission) sorter.getValueAt(selectedRow, -1);
					new GenericReportAdmission(adm.getId(), adm.getPatient().getCode(), GeneralData.ADMCHART);
				} else {
					MessageDialog.error(PatientFolderBrowser.this, "angal.admission.patientfolder.pleaseselectanadmission.msg");
				}
			});
		}
		return admReportButton;
	}

	private JButton getLaunchReportButton() {
		if (launchReportButton == null) {
			launchReportButton = new JButton(MessageBundle.getMessage("angal.common.launchreport.btn"));
			launchReportButton.setMnemonic(MessageBundle.getMnemonic("angal.common.launchreport.btn.key"));
			launchReportButton.addActionListener(actionEvent -> {
				if (olderDate == null) {
					MessageDialog.error(PatientFolderBrowser.this, "angal.admission.patientfolder.nodatatoshow.msg");
					return;
				}
				new PatientFolderReportModal(PatientFolderBrowser.this, patient.getCode(),olderDate);
			});
		}
		return launchReportButton;
	}

    DicomGui dg = null;

    public void resetDicomViewer()
    {
        dg = null;
    }

	private JButton getDICOMButton() {
		if (dicomButton == null) {
			dicomButton = new JButton(MessageBundle.getMessage("angal.admission.patientfolder.dicom.btn"));
			dicomButton.setMnemonic(MessageBundle.getMnemonic("angal.admission.patientfolder.dicom.btn.key"));
			dicomButton.addActionListener(actionEvent -> {
				if (dg == null) {
					dg = new DicomGui(patient, PatientFolderBrowser.this);
				}
			});
		}
		return dicomButton;
	}

	private JButton getCloseButton() {
		if (closeButton == null) {
			closeButton = new JButton(MessageBundle.getMessage("angal.common.close.btn"));
			closeButton.setMnemonic(MessageBundle.getMnemonic("angal.common.close.btn.key"));
			closeButton.addActionListener(actionEvent -> dispose());
		}
		return closeButton;
	}

	private <T> void getOlderDate(List<T> list, String variableName) {
		for (Object obj : list) {
			GregorianCalendar otherDate = getDateFromObject(obj, variableName);
			if (olderDate == null || olderDate.after(otherDate)) {
				olderDate = otherDate;
			}
		}
	}

	private GregorianCalendar getDateFromObject(Object obj, String variableName) {
		GregorianCalendar date = null;
		try {
			PropertyDescriptor pd = new PropertyDescriptor(variableName, obj.getClass());
			Method getter = pd.getReadMethod();
			date = (GregorianCalendar) getter.invoke(obj);

		} catch (IntrospectionException | InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
			LOGGER.error(e.getMessage(), e);
		}
		return date;
	}

	class AdmissionBrowserModel extends DefaultTableModel {

		private static final long serialVersionUID = -453243229156512947L;
		private AdmissionBrowserManager manager = Context.getApplicationContext().getBean(AdmissionBrowserManager.class);
		private DiseaseBrowserManager dbm = Context.getApplicationContext().getBean(DiseaseBrowserManager.class);
		private WardBrowserManager wbm = Context.getApplicationContext().getBean(WardBrowserManager.class);
		private OpdBrowserManager opd = Context.getApplicationContext().getBean(OpdBrowserManager.class);
		private ExaminationBrowserManager examin = Context.getApplicationContext().getBean(ExaminationBrowserManager.class);

		public AdmissionBrowserModel() {

			try {
				admList = manager.getAdmissions(patient);
				getOlderDate(admList, "admDate");
			} catch (OHServiceException e) {
				OHServiceExceptionUtil.showMessages(e);
			}
			try {
				disease = dbm.getDiseaseAll();
			} catch (OHServiceException e) {
				OHServiceExceptionUtil.showMessages(e);
			}
			try {
				ward = wbm.getWards();
			} catch (OHServiceException e) {
				OHServiceExceptionUtil.showMessages(e);
			}
			try {
				opdList = opd.getOpdList(patient.getCode());
				getOlderDate(opdList, "visitDate");
			} catch (OHServiceException e) {
				OHServiceExceptionUtil.showMessages(e);
			}
			try {
				examinationList = examin.getByPatID(patient.getCode());
				getOlderDate(examinationList, "pex_date");
			} catch (OHServiceException e) {
				OHServiceExceptionUtil.showMessages(e);
			}
		}

		@Override
		public int getRowCount() {
			int count = 0;
			if (admList != null) {
				count += admList.size();
			}
			if (opdList != null) {
				count += opdList.size();
			}
			if (examinationList != null) {
				count += examinationList.size();
			}
			return count;
		}

		@Override
		public String getColumnName(int c) {
			return pColumns[c];
		}

		@Override
		public int getColumnCount() {
			return pColumns.length;
		}

		@Override
		public Object getValueAt(int row, int column) {

			if (column == -1) {
				if (row < admList.size()) {
					return admList.get(row);
				} else if (row < opdList.size()+admList.size()) {
					 int z = row - admList.size();
					return opdList.get(z);
				} else {
					int f = row - (opdList.size()+admList.size());
					return examinationList.get(f);
				}

			} else if (column == 0) {
				if (row < admList.size()) {

					DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
					Date myDate = (admList.get(row)).getAdmDate().getTime();
					String strDate = dateFormat.format(myDate);

					return strDate;

				} else if (row < opdList.size()+admList.size()) {
					int z = row - admList.size();
					DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
					Date myDate = (opdList.get(z)).getVisitDate().getTime();
					String strDate = dateFormat.format(myDate);

					return strDate;

				} else {
					int f = row - (opdList.size()+admList.size());
					GregorianCalendar cal = examinationList.get(f).getPex_date();
					DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");
					Date myDate = cal.getTime();
					String strDate = dateFormat.format(myDate);

					return strDate;

				}

			} else if (column == 1) {
				if (row < admList.size()) {
					String id = admList.get(row).getWard().getCode();
					for (Ward elem : ward) {
						if (elem.getCode().equalsIgnoreCase(id)) {
							return elem.getDescription();
						}
					}
				} else if (row < opdList.size() + admList.size()) {
					return MessageBundle.getMessage("angal.admission.patientfolder.opd.txt");
				} else {
					return "EXAMINATION";
				}
			} else if (column == 2) {
				String id;
				if (row < admList.size()) {
					id = admList.get(row).getDiseaseIn().getCode();
					if (id == null) {
						id = "";
					}
					for (Disease elem : disease) {
						if (elem.getCode().equalsIgnoreCase(id)) {
							return elem.getDescription();
						}
					}
					return MessageBundle.getMessage("angal.admission.nodisease.txt");
				} else if (row < opdList.size() + admList.size()) {
					int z = row - admList.size();
					id = opdList.get(z).getDisease().getCode();
					if (id == null) {
						id = "";
					}
					for (Disease elem : disease) {
						if (elem.getCode().equalsIgnoreCase(id)) {
							return elem.getDescription();
						}
					}
					return MessageBundle.getMessage("angal.admission.nodisease.txt");
				} else {
					int f = row - (opdList.size() + admList.size());
					String ret = "<html>" +
							MessageBundle.getMessage("angal.common.weight.txt") + ": " + (examinationList.get(f).getPex_height())
							+ "<br>" +
							MessageBundle.getMessage("angal.common.height.txt") + ": " + (examinationList.get(f).getPex_weight())
							+ "</html>";
					return ret;
				}

			} else if (column == 3) {
				String id;
				if (row < admList.size()) {
					id = admList.get(row).getDiseaseOut1() == null ? null : admList.get(row).getDiseaseOut1().getCode();
					if (id == null) {
						id = "";
					}
					for (Disease elem : disease) {
						if (elem.getCode().equalsIgnoreCase(id)) {
							return elem.getDescription();
						}
					}
					return MessageBundle.getMessage("angal.admission.nodisease.txt");
				} else if (row < opdList.size() + admList.size()) {
					int z = row - admList.size();
					Disease dis = opdList.get(z).getDisease3();
					if (dis == null) {
						dis = opdList.get(z).getDisease2();
						if (dis == null) {
							id = opdList.get(z).getDisease().getCode();
						} else {
							id = dis.getCode();
						}
					} else {
						id = dis.getCode();
					}
					for (Disease elem : disease) {
						if (elem.getCode().equalsIgnoreCase(id)) {
							return elem.getDescription();
						}
					}
					return MessageBundle.getMessage("angal.admission.nodisease.txt");
				} else {
					int f = row - (opdList.size() + admList.size());
					String ret = "<html>" +
							MessageBundle.getMessage("angal.common.arterialpressureabbr.txt") + ": " + (examinationList.get(f).getPex_ap_min())
							+ '/' + (examinationList.get(f).getPex_ap_max())
							+ "<br>" +
							MessageBundle.getMessage("angal.common.temperatureabbr.txt") + ": " + (examinationList.get(f).getPex_temp()) +
							"</html>";
					return ret;
				}
			}  else if (column == 4) {
				if (row < admList.size()) {
					if (admList.get(row).getDisDate()==null) {
						return MessageBundle.getMessage("angal.admission.present.txt");
					} else {
						Date myDate = admList.get(row).getDisDate().getTime();
						return myDate;
					}
				} else if (row < opdList.size() + admList.size()) {
					int z = row - admList.size();
					String status = "" + opdList.get(z).getNewPatient();
					return (status.compareTo("R") == 0
							? MessageBundle.getMessage("angal.opd.reattendance.txt")
							: MessageBundle.getMessage("angal.opd.newattendance.txt"));
				} else {
					int f = row - (opdList.size() + admList.size());
					String ret = MessageBundle.getMessage("angal.admission.o2.txt") + ": " + (examinationList.get(f).getPex_sat());
					return ret;
				}
			}
			return null;
		}

		@Override
		public boolean isCellEditable(int arg0, int arg1) {
			return false;
		}
	}
	
	class LabBrowserModel extends DefaultTableModel {

		private static final long serialVersionUID = -8245833681073162426L;

		public LabBrowserModel() {
			LabManager lbm = Context.getApplicationContext().getBean(LabManager.class,Context.getApplicationContext().getBean(LabIoOperations.class));
			try {
				labList = lbm.getLaboratory(patient);
				getOlderDate(labList, "examDate");
			} catch (OHServiceException e) {
				labList = new ArrayList<>();
                OHServiceExceptionUtil.showMessages(e);
			}
		}
		
		@Override
		public int getRowCount() {
			if (labList == null) {
				return 0;
			}
			return labList.size();
		}

		@Override
		public String getColumnName(int c) {
			return plColumns[c];
		}

		@Override
		public int getColumnCount() {
			return plColumns.length;
		}
		
		@Override
		public Object getValueAt(int row, int column) {
			Laboratory laboratory = labList.get(row);
			if (column == -1) {
				return laboratory;
			} else if (column == 0) {
				Date examDate = laboratory.getExamDate().getTime();
				return examDate;
			} else if (column == 1) {
				return laboratory.getExam().getDescription();
			}else if (column == 2) {
				return laboratory.getCode();
			} else if (column == 3) {
				return laboratory.getResult();
			}
			return null;
		}

		@Override
		public boolean isCellEditable(int arg0, int arg1) {
			return false;
		}
	}

	public class DateCellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if (value instanceof Date) {
				// Use SimpleDateFormat class to get a formatted String from Date object.
				String strDate = new SimpleDateFormat(DATE_FORMAT).format((Date) value);

				// Sorting algorithm will work with model value. So you dont need to worry
				// about the renderer's display value. 
				this.setText(strDate);
			}
			return this;
		}
	}

	private void updateRowHeights() {
		for (int row = 0; row < admTable.getRowCount(); row++) {
			int rowHeight = admTable.getRowHeight();

			for (int column = 0; column < admTable.getColumnCount(); column++) {
				Component comp = admTable.prepareRenderer(admTable.getCellRenderer(row, column), row, column);
				rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
			}

			admTable.setRowHeight(row, rowHeight);
		}
	}
	
}
