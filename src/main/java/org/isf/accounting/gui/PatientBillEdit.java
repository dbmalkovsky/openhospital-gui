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
package org.isf.accounting.gui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EventListener;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.isf.accounting.manager.BillBrowserManager;
import org.isf.accounting.model.Bill;
import org.isf.accounting.model.BillItems;
import org.isf.accounting.model.BillPayments;
import org.isf.accounting.service.AccountingIoOperations;
import org.isf.generaldata.GeneralData;
import org.isf.generaldata.MessageBundle;
import org.isf.generaldata.TxtPrinter;
import org.isf.hospital.manager.HospitalBrowsingManager;
import org.isf.menu.gui.MainMenu;
import org.isf.menu.manager.Context;
import org.isf.menu.manager.UserBrowsingManager;
import org.isf.patient.gui.SelectPatient;
import org.isf.patient.gui.SelectPatient.SelectionListener;
import org.isf.patient.manager.PatientBrowserManager;
import org.isf.patient.model.Patient;
import org.isf.priceslist.manager.PriceListManager;
import org.isf.priceslist.model.Price;
import org.isf.priceslist.model.PriceList;
import org.isf.pricesothers.manager.PricesOthersManager;
import org.isf.pricesothers.model.PricesOthers;
import org.isf.stat.gui.report.GenericReportBill;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.gui.OHServiceExceptionUtil;
import org.isf.utils.jobjects.CustomJDateChooser;
import org.isf.utils.jobjects.MessageDialog;
import org.isf.utils.time.RememberDates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create a single Patient Bill
 * it affects tables BILLS, BILLITEMS and BILLPAYMENTS
 *
 * @author Mwithi
 */
public class PatientBillEdit extends JDialog implements SelectionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(PatientBillEdit.class);

//LISTENER INTERFACE --------------------------------------------------------
	private EventListenerList patientBillListener = new EventListenerList();
	
	public interface PatientBillListener extends EventListener {
		void billInserted(AWTEvent aEvent);
	}
	
	public void addPatientBillListener(PatientBillListener l) {
		patientBillListener.add(PatientBillListener.class, l);
	}

	private void fireBillInserted(Bill aBill) {
		AWTEvent event = new AWTEvent(aBill, AWTEvent.RESERVED_ID_MAX + 1) {

			private static final long serialVersionUID = 1L;
		};

		EventListener[] listeners = patientBillListener.getListeners(PatientBillListener.class);
		for (EventListener listener : listeners) {
			((PatientBillListener) listener).billInserted(event);
		}
	}

	@Override
	public void patientSelected(Patient patient) {
		// patientSelected = patient;
		setPatientSelected(patient);
		List<Bill> patientPendingBills = new ArrayList<>();
		try {
			patientPendingBills = billManager.getPendingBills(patient.getCode());
		} catch (OHServiceException ohServiceException) {
			LOGGER.error(ohServiceException.getMessage(), ohServiceException);
		}
		if (patientPendingBills.isEmpty()) {
			// BILL
			thisBill.setBillPatient(patientSelected);
			thisBill.setIsPatient(true);
			thisBill.setPatName(patientSelected.getName());
		} else {
			if (patientPendingBills.size() == 1) {
				if (GeneralData.ALLOWMULTIPLEOPENEDBILL) {
					int response = MessageDialog.yesNo(PatientBillEdit.this,
							"angal.newbill.thispatienthasapendingbilldoyouwanttocreateanother.msg");
					if (response == JOptionPane.YES_OPTION) {
						insert = true;
						//thisBill.setPatID(patientSelected.getCode());
						thisBill.setBillPatient(patientSelected);
						thisBill.setIsPatient(true);
						thisBill.setPatName(patientSelected.getName());
					} else {
						insert = false;
						setBill(patientPendingBills.get(0));

						/* ****** Check if it is same month ************** */
						//checkIfsameMonth();
						/* *********************************************** */
					}
				} else {
					MessageDialog.error(null, "angal.newbill.thispatienthasapendingbill.msg");
					insert = false;
					setBill(patientPendingBills.get(0));

					/* ****** Check if it is same month ************** */
					//checkIfsameMonth();
					/* *********************************************** */
				}
			} else {
				if (GeneralData.ALLOWMULTIPLEOPENEDBILL) {
					int response = MessageDialog.yesNo(PatientBillEdit.this,
							"angal.newbill.thispatienthasmorethanonependingbilldoyouwanttocreateanother.msg");
					if (response == JOptionPane.YES_OPTION) {
						insert = true;
						//thisBill.setPatID(patientSelected.getCode());
						thisBill.setBillPatient(patientSelected);
						thisBill.setIsPatient(true);
						thisBill.setPatName(patientSelected.getName());
					} else if (response == JOptionPane.NO_OPTION) {
						// something must be proposed
						int resp = MessageDialog.yesNo(PatientBillEdit.this,
								"angal.newbill.thispatienthasmorethanonependingbilldoyouwanttoopenthelastpendingbill.msg");
						if (resp == JOptionPane.YES_OPTION) {
							insert = false;
							setBill(patientPendingBills.get(0));
							/* ****** Check if it is same month ************** */
							//checkIfsameMonth();
							/* *********************************************** */
						} else {
							dispose();
						}
					} else {
						return;
					}
				} else {
					MessageDialog.yesNo(null, "angal.admission.thereismorethanonependingbillforthispatientcontinue.msg");
					// TODO: the response is not checked; something needs to be done here
					return;
				}				
			}
		}
		updateUI();
	}

	private static final long serialVersionUID = 1L;
	private JTable jTableBill;
	private JScrollPane jScrollPaneBill;
	private JButton jButtonAddMedical;
	private JButton jButtonAddOperation;
	private JButton jButtonAddExam;
	private JButton jButtonAddOther;
	private JButton jButtonAddPayment;
	private JPanel jPanelButtons;
	private JPanel jPanelDate;
	private JPanel jPanelPatient;
	private JTable jTablePayment;
	private JScrollPane jScrollPanePayment;
	private JTextField jTextFieldPatient;
	private JComboBox<PriceList> jComboBoxPriceList;
	private JPanel jPanelData;
	private JTable jTableTotal;
	private JScrollPane jScrollPaneTotal;
	private JTable jTableBigTotal;
	private JScrollPane jScrollPaneBigTotal;
	private JTable jTableBalance;
	private JScrollPane jScrollPaneBalance;
	private JPanel jPanelTop;
	private CustomJDateChooser jCalendarDate;
	private JLabel jLabelDate;
	private JLabel jLabelUser;
	private JLabel jLabelPatient;
	private JButton jButtonRemoveItem;
	private JLabel jLabelPriceList;
	private JButton jButtonRemovePayment;
	private JButton jButtonAddRefund;
	private JPanel jPanelButtonsPayment;
	private JPanel jPanelButtonsBill;
	private JPanel jPanelButtonsActions;
	private JButton jButtonClose;
	private JButton jButtonPaid;
	private JButton jButtonPrintPayment;
	private JButton jButtonSave;
	private JButton jButtonBalance;
	private JButton jButtonCustom;
	private JButton jButtonPickPatient;
	private JButton jButtonTrashPatient;
	
	private static final Dimension PatientDimension = new Dimension(300,20);
	private static final Dimension LabelsDimension = new Dimension(60,20);
	private static final Dimension UserDimension = new Dimension(190,20);
	private static final int PanelWidth = 450;
	private static final int ButtonWidth = 190;
	private static final int ButtonWidthBill = 190;
	private static final int ButtonWidthPayment = 190;
	private static final int PriceWidth = 190;
	private static final int CurrencyCodWidth = 40;
	private static final int QuantityWidth = 40;
	private static final int BillHeight = 200;
	private static final int TotalHeight = 20;
	private static final int BigTotalHeight = 20;
	private static final int PaymentHeight = 150;
	private static final int BalanceHeight = 20;
	private static final int ButtonHeight = 25;
	
	private BigDecimal total = new BigDecimal(0);
	private BigDecimal bigTotal = new BigDecimal(0);
	private BigDecimal balance = new BigDecimal(0);
	private int billID;
	private PriceList listSelected;
	private boolean insert;
	private boolean modified = false;
	private boolean keepDate = true;
	private boolean paid = false;
	private Bill thisBill;
	private Patient patientSelected;
	private boolean foundList;
	private GregorianCalendar billDate = new GregorianCalendar();
	private GregorianCalendar today = new GregorianCalendar();
	
	private Object[] billClasses = {Price.class, Integer.class, Double.class};
	private String[] billColumnNames = {
			MessageBundle.getMessage("angal.newbill.item.col").toUpperCase(),
			MessageBundle.getMessage("angal.common.qty.txt").toUpperCase(),
			MessageBundle.getMessage("angal.common.amount.txt").toUpperCase()
	};
	private Object[] paymentClasses = {Date.class, Double.class};
	
	private String currencyCod;
	
	//Prices and Lists (ALL)
	private PriceListManager prcManager = Context.getApplicationContext().getBean(PriceListManager.class);
	private List<Price> prcArray;
	private List<PriceList> lstArray;
	
	//PricesOthers (ALL)
	private PricesOthersManager othManager = Context.getApplicationContext().getBean(PricesOthersManager.class);
	private List<PricesOthers> othPrices;

	//Items and Payments (ALL)
	private BillBrowserManager billManager = new BillBrowserManager(Context.getApplicationContext().getBean(AccountingIoOperations.class));
	private PatientBrowserManager patManager = Context.getApplicationContext().getBean(PatientBrowserManager.class);
	
	//Prices, Items and Payments for the tables
	private List<BillItems> billItems = new ArrayList<>();
	private List<BillPayments> payItems = new ArrayList<>();
	private ArrayList<Price> prcListArray = new ArrayList<>();
	private int billItemsSaved;
	private int payItemsSaved;
	
	//User
	private String user = UserBrowsingManager.getCurrentUser();
	
	public PatientBillEdit() {
		initCurrencyCod();
		PatientBillEdit newBill = new PatientBillEdit(null, new Bill(), true);
		newBill.setVisible(true);
		try {
			prcArray = prcManager.getPrices();
			lstArray = prcManager.getLists();
			othPrices = othManager.getOthers();
		} catch (OHServiceException e) {
			OHServiceExceptionUtil.showMessages(e, PatientBillEdit.this);
		}
	}
	
	public PatientBillEdit(JFrame owner, Patient patient) {
		initCurrencyCod();
		Bill bill = new Bill();
		bill.setIsPatient(true);
		bill.setBillPatient(patient);
		bill.setPatName(patient.getName());
		PatientBillEdit newBill = new PatientBillEdit(owner, bill, true);
		newBill.setPatientSelected(patient);
		newBill.setVisible(true);
		try {
			prcArray = prcManager.getPrices();
			lstArray = prcManager.getLists();
			othPrices = othManager.getOthers();
		} catch (OHServiceException e) {
			OHServiceExceptionUtil.showMessages(e, PatientBillEdit.this);
		}
	}
	
	public PatientBillEdit(JFrame owner, Bill bill, boolean inserting) {
		super(owner, true);
		initCurrencyCod();
		this.insert = inserting;
		try {
			prcArray = prcManager.getPrices();
			lstArray = prcManager.getLists();
			othPrices = othManager.getOthers();
		} catch (OHServiceException e) {
			OHServiceExceptionUtil.showMessages(e, PatientBillEdit.this);
		}
		setBill(bill);
		initComponents();
		updateTotals();
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);
		setResizable(false);
	}
	
	private void initCurrencyCod() {
		try {
			this.currencyCod = Context.getApplicationContext().getBean(HospitalBrowsingManager.class).getHospitalCurrencyCod();
		} catch (OHServiceException e) {
			this.currencyCod = null;
			OHServiceExceptionUtil.showMessages(e, PatientBillEdit.this);
		}
	}

	private void setBill(Bill bill) {
		this.thisBill = bill;
		billDate = bill.getDate();
		try {
			billItems = billManager.getItems(thisBill.getId());
			payItems = billManager.getPayments(thisBill.getId());
			othPrices = othManager.getOthers();
		} catch (OHServiceException e) {
			OHServiceExceptionUtil.showMessages(e, PatientBillEdit.this);
		}
		billItemsSaved = billItems.size();
		payItemsSaved = payItems.size();
		if (!insert) {
			checkBill();
		}
	}
	
	private void initComponents() {
		add(getJPanelTop(), BorderLayout.NORTH);
		add(getJPanelData(), BorderLayout.CENTER);
		add(getJPanelButtons(), BorderLayout.EAST);
		if (insert) {
			setTitle(MessageBundle.getMessage("angal.patientbill.newpatientbill.title"));
		} else {
			setTitle(MessageBundle.formatMessage("angal.patientbill.editpatientbill.fmt.title", thisBill.getId()));
		}
		pack();
	}

	//check if PriceList and Patient still exist
	private void checkBill() {
		
		foundList = false;
		if (thisBill.isList()) {
			for (PriceList list : lstArray) {
				
				if (list.getId() == thisBill.getPriceList().getId()) {
					listSelected = list;
					foundList = true;
					break;
				}
			}
			if (!foundList) { //PriceList not found
				Icon icon = new ImageIcon("rsc/icons/list_dialog.png"); //$NON-NLS-1$
				PriceList list = (PriceList)JOptionPane.showInputDialog(
				                    PatientBillEdit.this,
				                    MessageBundle.getMessage("angal.newbill.thepricelistassociatedwiththisbillnolongerexists.msg"),
				                    MessageBundle.getMessage("angal.newbill.selectapricelist.title"),
				                    JOptionPane.OK_OPTION,
				                    icon,
				                    lstArray.toArray(),
				                    "");
				if (list == null) {
					MessageDialog.warning(PatientBillEdit.this, "angal.newbill.nopricelistselectedwilbeused.fmt.msg",
							lstArray.get(0).getName());
					list = lstArray.get(0);
				}
				thisBill.setPriceList(list);
				thisBill.setListName(list.getName());
			}
		}
				
		if (thisBill.isPatient()) {
			
			Patient patient = null;
			try {
				patient = patManager.getPatientById(thisBill.getBillPatient().getCode());
			} catch (OHServiceException ohServiceException) {
				MessageDialog.showExceptions(ohServiceException);
			}
			if (patient != null) {
				patientSelected = patient;
			} else {  //Patient not found
				MessageDialog.warning(PatientBillEdit.this, "angal.newbill.patientassociatedwiththisbillnolongerexists.msg");
				thisBill.setIsPatient(false);
				thisBill.getBillPatient().setCode(0);
			}
		}
	}
	
	private JPanel getJPanelData() {
		if (jPanelData == null) {
			jPanelData = new JPanel();
			jPanelData.setLayout(new BoxLayout(jPanelData, BoxLayout.Y_AXIS));
			jPanelData.add(getJScrollPaneTotal());
			jPanelData.add(getJScrollPaneBill());
			jPanelData.add(getJScrollPaneBigTotal());
			jPanelData.add(getJScrollPanePayment());
			jPanelData.add(getJScrollPaneBalance());
		}
		return jPanelData;
	}
	
	private JPanel getJPanelPatient() {
		if (jPanelPatient == null) {
			jPanelPatient = new JPanel();
			jPanelPatient.setLayout(new FlowLayout(FlowLayout.LEFT));
			jPanelPatient.add(getJLabelPatient());
			jPanelPatient.add(getJTextFieldPatient());
			jPanelPatient.add(getJLabelPriceList());
			jPanelPatient.add(getJComboBoxPriceList());
		}
		return jPanelPatient;
	}

	private JLabel getJLabelPatient() {
		if (jLabelPatient == null) {
			jLabelPatient = new JLabel(MessageBundle.getMessage("angal.common.patient.txt"));
			jLabelPatient.setPreferredSize(LabelsDimension);
		}
		return jLabelPatient;
	}

	
	private JTextField getJTextFieldPatient() {
		if (jTextFieldPatient == null) {
			jTextFieldPatient = new JTextField();
			jTextFieldPatient.setText(""); //$NON-NLS-1$
			jTextFieldPatient.setPreferredSize(PatientDimension);
			//Font patientFont=new Font(jTextFieldPatient.getFont().getName(), Font.BOLD, jTextFieldPatient.getFont().getSize() + 4);
			//jTextFieldPatient.setFont(patientFont);
			//if (!insert) jTextFieldPatient.setText(thisBill.getPatName());
			if (thisBill.isPatient()) {
				jTextFieldPatient.setText(thisBill.getPatName());
			}
			jTextFieldPatient.setEditable(false);
		}
		return jTextFieldPatient;
	}
	
	private JLabel getJLabelPriceList() {
		if (jLabelPriceList == null) {
			jLabelPriceList = new JLabel(MessageBundle.getMessage("angal.newbill.list.txt"));
		}
		return jLabelPriceList;
	}
	
	private JComboBox getJComboBoxPriceList() {
		if (jComboBoxPriceList == null) {
			jComboBoxPriceList = new JComboBox<>();
			PriceList list = null;
			for (PriceList lst : lstArray) {
				
				jComboBoxPriceList.addItem(lst);
				if (!insert) {
					if (lst.getId() == thisBill.getPriceList().getId()) {
						list = lst;
					}
				}
			}
			if (list != null) {
				jComboBoxPriceList.setSelectedItem(list);
			}
			
			jComboBoxPriceList.addActionListener(actionEvent -> {

				listSelected = (PriceList)jComboBoxPriceList.getSelectedItem();
				jTableBill.setModel(new BillTableModel());
				updateTotals();
			});
		}
		return jComboBoxPriceList;
	}
	
	private CustomJDateChooser getJCalendarDate() {
		if (jCalendarDate == null) {
			
			if (insert) {
				//To remind last used
				billDate.set(Calendar.YEAR, RememberDates.getLastBillDateGregorian().get(Calendar.YEAR));
				billDate.set(Calendar.MONTH, RememberDates.getLastBillDateGregorian().get(Calendar.MONTH));
				billDate.set(Calendar.DAY_OF_MONTH, RememberDates.getLastBillDateGregorian().get(Calendar.DAY_OF_MONTH));
				jCalendarDate = new CustomJDateChooser(billDate.getTime()); 
			} else { 
				//get BillDate
				jCalendarDate = new CustomJDateChooser(thisBill.getDate().getTime());
				billDate.setTime(jCalendarDate.getDate());
			}
			jCalendarDate.setLocale(new Locale(GeneralData.LANGUAGE));
			jCalendarDate.setDateFormatString("dd/MM/yy - HH:mm:ss"); //$NON-NLS-1$
			jCalendarDate.getJCalendar().addPropertyChangeListener("calendar", propertyChangeEvent -> {

				if (!insert) {
					if (keepDate && propertyChangeEvent.getNewValue().toString().compareTo(propertyChangeEvent.getOldValue().toString()) != 0) {

						int ok = MessageDialog.yesNo(PatientBillEdit.this, "angal.newbill.doyouwanttochangetheoriginaldate.msg");
						if (ok == JOptionPane.YES_OPTION) {
							keepDate = false;
							modified = true;
							jCalendarDate.setDate(((Calendar)propertyChangeEvent.getNewValue()).getTime());
						} else {
							jCalendarDate.setDate(((Calendar)propertyChangeEvent.getOldValue()).getTime());
						}
					} else {
						jCalendarDate.setDate(((Calendar)propertyChangeEvent.getNewValue()).getTime());
					}
				} else {
					jCalendarDate.setDate(((Calendar)propertyChangeEvent.getNewValue()).getTime());
				}
				billDate.setTime(jCalendarDate.getDate());
			});
		}
		return jCalendarDate;
	}
	
	private JLabel getJLabelDate() {
		if (jLabelDate == null) {
			jLabelDate = new JLabel(MessageBundle.getMessage("angal.common.date.txt"));
			jLabelDate.setPreferredSize(LabelsDimension);
		}
		return jLabelDate;
	}

	private JPanel getJPanelDate() {
		if (jPanelDate == null) {
			jPanelDate = new JPanel();
			jPanelDate.setLayout(new FlowLayout(FlowLayout.LEFT));
			jPanelDate.add(getJLabelDate());
			jPanelDate.add(getJCalendarDate());
			jPanelDate.add(getJButtonPickPatient());
			jPanelDate.add(getJButtonTrashPatient());
			if (!GeneralData.getGeneralData().getSINGLEUSER()) {
				jPanelDate.add(getJLabelUser());
			}
		}
		return jPanelDate;
	}

	private JLabel getJLabelUser() {
		if (jLabelUser == null) {
			jLabelUser = new JLabel(MainMenu.getUser().getUserName());
			jLabelUser.setPreferredSize(UserDimension);
			jLabelUser.setHorizontalAlignment(SwingConstants.RIGHT);
			jLabelUser.setForeground(Color.BLUE);
			jLabelUser.setFont(new Font(jLabelUser.getFont().getName(), Font.BOLD, jLabelUser.getFont().getSize() + 2));
		}
		return jLabelUser;
	}

	private JButton getJButtonTrashPatient() {
		if (jButtonTrashPatient == null) {
			jButtonTrashPatient = new JButton();
			jButtonTrashPatient.setPreferredSize(new Dimension(25,25));
			jButtonTrashPatient.setIcon(new ImageIcon("rsc/icons/remove_patient_button.png"));
			jButtonTrashPatient.setToolTipText(MessageBundle.getMessage("angal.newbill.removethepatientassociatedwiththisbill.tooltip"));
			jButtonTrashPatient.addActionListener(actionEvent -> {

				patientSelected = null;
				//BILL
				thisBill.setIsPatient(false);
				thisBill.getBillPatient().setCode(0);
				thisBill.setPatName(""); //$NON-NLS-1$
				//INTERFACE
				jTextFieldPatient.setText("");
				jTextFieldPatient.setEditable(false);
				jButtonPickPatient.setText(MessageBundle.getMessage("angal.newbill.findpatient.btn"));
				jButtonPickPatient.setToolTipText(MessageBundle.getMessage("angal.newbill.associateapatientwiththisbill.tooltip"));
				jButtonTrashPatient.setEnabled(false);
			});
			if (!thisBill.isPatient()) {
				jButtonTrashPatient.setEnabled(false);
			}
		}
		return jButtonTrashPatient;
	}

	private JButton getJButtonPickPatient() {
		if (jButtonPickPatient == null) {
			jButtonPickPatient = new JButton(MessageBundle.getMessage("angal.newbill.findpatient.btn"));
			jButtonPickPatient.setMnemonic(MessageBundle.getMnemonic("angal.newbill.findpatient.btn.key"));
			jButtonPickPatient.setIcon(new ImageIcon("rsc/icons/pick_patient_button.png"));
			jButtonPickPatient.setToolTipText(MessageBundle.getMessage("angal.newbill.associateapatientwiththisbill.tooltip"));
			jButtonPickPatient.addActionListener(actionEvent -> {

				SelectPatient sp = new SelectPatient(PatientBillEdit.this, patientSelected);
				sp.addSelectionListener(PatientBillEdit.this);
				sp.pack();
				sp.setVisible(true);

			});
			if (thisBill.isPatient()) {
				jButtonPickPatient.setText(MessageBundle.getMessage("angal.newbill.changepatient.btn"));
				jButtonPickPatient.setMnemonic(MessageBundle.getMnemonic("angal.newbill.changepatient.btn.key"));
				jButtonPickPatient.setToolTipText(MessageBundle.getMessage("angal.newbill.changethepatientassociatedwiththisbill.tooltip"));
			}
		}
		return jButtonPickPatient;
	}

	public void setPatientSelected(Patient patientSelected) {
		this.patientSelected = patientSelected;
	}

	private JPanel getJPanelTop() {
		if (jPanelTop == null) {
			jPanelTop = new JPanel();
			jPanelTop.setLayout(new BoxLayout(jPanelTop, BoxLayout.Y_AXIS));
			jPanelTop.add(getJPanelDate());
			jPanelTop.add(getJPanelPatient());
		}
		return jPanelTop;
	}

	private JScrollPane getJScrollPaneBill() {
		if (jScrollPaneBill == null) {
			jScrollPaneBill = new JScrollPane();
			jScrollPaneBill.setBorder(null);
			jScrollPaneBill.setViewportView(getJTableBill());
			jScrollPaneBill.setMaximumSize(new Dimension(PanelWidth, BillHeight));
			jScrollPaneBill.setMinimumSize(new Dimension(PanelWidth, BillHeight));
			jScrollPaneBill.setPreferredSize(new Dimension(PanelWidth, BillHeight));

		}
		return jScrollPaneBill;
	}

	private JTable getJTableBill() {
		if (jTableBill == null) {
			jTableBill = new JTable();
			jTableBill.setModel(new BillTableModel());
			jTableBill.getColumnModel().getColumn(1).setMinWidth(QuantityWidth);
			jTableBill.getColumnModel().getColumn(1).setMaxWidth(QuantityWidth);
			jTableBill.getColumnModel().getColumn(2).setMinWidth(PriceWidth);
			jTableBill.getColumnModel().getColumn(2).setMaxWidth(PriceWidth);
			jTableBill.setAutoCreateColumnsFromModel(false);
		}
		return jTableBill;
	}
	
	private JScrollPane getJScrollPaneBigTotal() {
		if (jScrollPaneBigTotal == null) {
			jScrollPaneBigTotal = new JScrollPane();
			jScrollPaneBigTotal.setViewportView(getJTableBigTotal());
			jScrollPaneBigTotal.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
			jScrollPaneBigTotal.setMaximumSize(new Dimension(PanelWidth, BigTotalHeight));
			jScrollPaneBigTotal.setMinimumSize(new Dimension(PanelWidth, BigTotalHeight));
			jScrollPaneBigTotal.setPreferredSize(new Dimension(PanelWidth, BigTotalHeight));
		}
		return jScrollPaneBigTotal;
	}
	
	private JScrollPane getJScrollPaneTotal() {
		if (jScrollPaneTotal == null) {
			jScrollPaneTotal = new JScrollPane();
			jScrollPaneTotal.setViewportView(getJTableTotal());
			jScrollPaneTotal.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
			jScrollPaneTotal.setMaximumSize(new Dimension(PanelWidth, TotalHeight));
			jScrollPaneTotal.setMinimumSize(new Dimension(PanelWidth, TotalHeight));
			jScrollPaneTotal.setPreferredSize(new Dimension(PanelWidth, TotalHeight));
		}
		return jScrollPaneTotal;
	}
	
	private JTable getJTableBigTotal() {
		if (jTableBigTotal == null) {
			jTableBigTotal = new JTable();
			jTableBigTotal.setModel(new DefaultTableModel(new Object[][] {
					{
						"<html><b>" + MessageBundle.getMessage("angal.newbill.topay.txt") + "</b></html>",
						currencyCod,
						bigTotal}
					}, new String[] {"","", ""}) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				private static final long serialVersionUID = 1L;
				Class<?>[] types = new Class<?>[] { JLabel.class, JLabel.class, Double.class, };
	
				@Override
				public Class<?> getColumnClass(int columnIndex) {
					return types[columnIndex];
				}

				@Override
				public boolean isCellEditable(int row, int column) {
					return false;
				}
			});
			jTableBigTotal.getColumnModel().getColumn(1).setMinWidth(CurrencyCodWidth);
			jTableBigTotal.getColumnModel().getColumn(1).setMaxWidth(CurrencyCodWidth);
			jTableBigTotal.getColumnModel().getColumn(2).setMinWidth(PriceWidth);
			jTableBigTotal.getColumnModel().getColumn(2).setMaxWidth(PriceWidth);
			jTableBigTotal.setMaximumSize(new Dimension(PanelWidth, BigTotalHeight));
			jTableBigTotal.setMinimumSize(new Dimension(PanelWidth, BigTotalHeight));
			jTableBigTotal.setPreferredSize(new Dimension(PanelWidth, BigTotalHeight));
		}
		return jTableBigTotal;
	}

	private JTable getJTableTotal() {
		if (jTableTotal == null) {
			jTableTotal = new JTable();
			jTableTotal.setModel(new DefaultTableModel(new Object[][] {
					{
						"<html><b>"+MessageBundle.getMessage("angal.common.total.txt").toUpperCase()+"</b></html>",
						currencyCod,
						total}
					}, 
					new String[] {"","", ""}) {
				private static final long serialVersionUID = 1L;
				Class<?>[] types = new Class<?>[] { JLabel.class, JLabel.class, Double.class, };
	
				@Override
				public Class<?> getColumnClass(int columnIndex) {
					return types[columnIndex];
				}

				@Override
				public boolean isCellEditable(int row, int column) {
					return false;
				}
			});
			jTableTotal.getColumnModel().getColumn(1).setMinWidth(CurrencyCodWidth);
			jTableTotal.getColumnModel().getColumn(1).setMaxWidth(CurrencyCodWidth);
			jTableTotal.getColumnModel().getColumn(2).setMinWidth(PriceWidth);
			jTableTotal.getColumnModel().getColumn(2).setMaxWidth(PriceWidth);
			jTableTotal.setMaximumSize(new Dimension(PanelWidth, TotalHeight));
			jTableTotal.setMinimumSize(new Dimension(PanelWidth, TotalHeight));
			jTableTotal.setPreferredSize(new Dimension(PanelWidth, TotalHeight));
		}
		return jTableTotal;
	}

	private JScrollPane getJScrollPanePayment() {
		if (jScrollPanePayment == null) {
			jScrollPanePayment = new JScrollPane();
			jScrollPanePayment.setBorder(null);
			jScrollPanePayment.setViewportView(getJTablePayment());
			jScrollPanePayment.setMaximumSize(new Dimension(PanelWidth, PaymentHeight));
			jScrollPanePayment.setMinimumSize(new Dimension(PanelWidth, PaymentHeight));
			jScrollPanePayment.setPreferredSize(new Dimension(PanelWidth, PaymentHeight));
		}
		return jScrollPanePayment;
	}

	private JTable getJTablePayment() {
		if (jTablePayment == null) {
			jTablePayment = new JTable();
			jTablePayment.setModel(new PaymentTableModel());
			jTablePayment.getColumnModel().getColumn(1).setMinWidth(PriceWidth);
			jTablePayment.getColumnModel().getColumn(1).setMaxWidth(PriceWidth);
		}
		return jTablePayment;
	}
	
	private JScrollPane getJScrollPaneBalance() {
		if (jScrollPaneBalance == null) {
			jScrollPaneBalance = new JScrollPane();
			jScrollPaneBalance.setViewportView(getJTableBalance());
			jScrollPaneBalance.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
			jScrollPaneBalance.setMaximumSize(new Dimension(PanelWidth, BalanceHeight));
			jScrollPaneBalance.setMinimumSize(new Dimension(PanelWidth, BalanceHeight));
			jScrollPaneBalance.setPreferredSize(new Dimension(PanelWidth, BalanceHeight));
		}
		return jScrollPaneBalance;
	}

	private JTable getJTableBalance() {
		if (jTableBalance == null) {
			jTableBalance = new JTable();
			jTableBalance.setModel(new DefaultTableModel(new Object[][] {
					{
						"<html><b>"+MessageBundle.getMessage("angal.newbill.balance.txt").toUpperCase()+"</b></html>",
						currencyCod,
						balance}
					}, 
					new String[] {"","",""}) {
				private static final long serialVersionUID = 1L;
				Class<?>[] types = new Class<?>[] { JLabel.class, JLabel.class, Double.class, };
	
				@Override
				public Class<?> getColumnClass(int columnIndex) {
					return types[columnIndex];
				}
				
				@Override
				public boolean isCellEditable(int row, int column) {
					return false;
				}
			});
			jTableBalance.getColumnModel().getColumn(1).setMinWidth(CurrencyCodWidth);
			jTableBalance.getColumnModel().getColumn(1).setMaxWidth(CurrencyCodWidth);
			jTableBalance.getColumnModel().getColumn(2).setMinWidth(PriceWidth);
			jTableBalance.getColumnModel().getColumn(2).setMaxWidth(PriceWidth);
			jTableBalance.setMaximumSize(new Dimension(PanelWidth, BalanceHeight));
			jTableBalance.setMinimumSize(new Dimension(PanelWidth, BalanceHeight));
			jTableBalance.setPreferredSize(new Dimension(PanelWidth, BalanceHeight));
		}
		return jTableBalance;
	}
	
	private JPanel getJPanelButtons() {
		if (jPanelButtons == null) {
			jPanelButtons = new JPanel();
			jPanelButtons.setLayout(new BoxLayout(jPanelButtons, BoxLayout.Y_AXIS));
			jPanelButtons.add(getJPanelButtonsBill());
			jPanelButtons.add(getJPanelButtonsPayment());
			jPanelButtons.add(Box.createVerticalGlue());
			jPanelButtons.add(getJPanelButtonsActions());
		}
		return jPanelButtons;
	}
	
	private JPanel getJPanelButtonsBill() {
		if (jPanelButtonsBill == null) {
			jPanelButtonsBill = new JPanel();
			jPanelButtonsBill.setLayout(new BoxLayout(jPanelButtonsBill, BoxLayout.Y_AXIS));
			jPanelButtonsBill.add(getJButtonAddMedical());
			jPanelButtonsBill.add(getJButtonAddOperation());
			jPanelButtonsBill.add(getJButtonAddExam());
			jPanelButtonsBill.add(getJButtonAddOther());
			jPanelButtonsBill.add(getJButtonAddCustom());
			jPanelButtonsBill.add(getJButtonRemoveItem());
			jPanelButtonsBill.setMinimumSize(new Dimension(ButtonWidth, BillHeight+TotalHeight));
			jPanelButtonsBill.setMaximumSize(new Dimension(ButtonWidth, BillHeight+TotalHeight));
			jPanelButtonsBill.setPreferredSize(new Dimension(ButtonWidth, BillHeight+TotalHeight));

		}
		return jPanelButtonsBill;
	}

	private JPanel getJPanelButtonsPayment() {
		if (jPanelButtonsPayment == null) {
			jPanelButtonsPayment = new JPanel();
			jPanelButtonsPayment.setLayout(new BoxLayout(jPanelButtonsPayment, BoxLayout.Y_AXIS));
			jPanelButtonsPayment.add(getJButtonAddPayment());
			jPanelButtonsPayment.add(getJButtonAddRefund());
			if (GeneralData.RECEIPTPRINTER) {
				jPanelButtonsPayment.add(getJButtonPrintPayment());
			}
			jPanelButtonsPayment.add(getJButtonRemovePayment());
			jPanelButtonsPayment.setMinimumSize(new Dimension(ButtonWidth, PaymentHeight));
			jPanelButtonsPayment.setMaximumSize(new Dimension(ButtonWidth, PaymentHeight));
			//jPanelButtonsPayment.setPreferredSize(new Dimension(ButtonWidth, PaymentHeight));
		}
		return jPanelButtonsPayment;
	}
	
	private JPanel getJPanelButtonsActions() {
		if (jPanelButtonsActions == null) {
			jPanelButtonsActions = new JPanel();
			jPanelButtonsActions.setLayout(new BoxLayout(jPanelButtonsActions, BoxLayout.Y_AXIS));
			jPanelButtonsActions.add(getJButtonBalance());
			jPanelButtonsActions.add(getJButtonSave());
			jPanelButtonsActions.add(getJButtonPaid());
			jPanelButtonsActions.add(getJButtonClose());
		}
		return jPanelButtonsActions;
	}

	private JButton getJButtonBalance() {
		if (jButtonBalance == null) {
			jButtonBalance = new JButton(MessageBundle.getMessage("angal.newbill.givechange.btn"));
			jButtonBalance.setMnemonic(MessageBundle.getMnemonic("angal.newbill.givechange.btn.key"));
			jButtonBalance.setMaximumSize(new Dimension(ButtonWidth, ButtonHeight));
			jButtonBalance.setIcon(new ImageIcon("rsc/icons/money_button.png"));
			jButtonBalance.setHorizontalAlignment(SwingConstants.LEFT);
			if (insert)  {
				jButtonBalance.setEnabled(false);
			}
			jButtonBalance.addActionListener(actionEvent -> {

				Icon icon = new ImageIcon("rsc/icons/money_dialog.png");
				BigDecimal amount = new BigDecimal(0);

				String quantity = (String) JOptionPane.showInputDialog(PatientBillEdit.this,
						MessageBundle.getMessage("angal.newbill.entercustomercash.txt"),
						MessageBundle.getMessage("angal.newbill.givechange.title"),
						JOptionPane.OK_CANCEL_OPTION,
						icon,
						null,
						amount);

				if (quantity != null) {
					try {
						amount = new BigDecimal(quantity);
						if (amount.equals(new BigDecimal(0)) || amount.compareTo(balance) < 0) {
							return;
						}
						JOptionPane.showMessageDialog(PatientBillEdit.this,
								MessageBundle.formatMessage("angal.newbill.givechange.fmt.msg", amount.subtract(balance)),
								MessageBundle.getMessage("angal.newbill.givechange.title"),
								JOptionPane.OK_OPTION,
								icon);
					} catch (Exception eee) {
						MessageDialog.error(PatientBillEdit.this, "angal.newbill.invalidquantitypleasetryagain.msg");
					}
				}
			});
		}
		return jButtonBalance;
	}

	private JButton getJButtonSave() {
		if (jButtonSave == null) {
			jButtonSave = new JButton(MessageBundle.getMessage("angal.common.save.btn"));
			jButtonSave.setMnemonic(MessageBundle.getMnemonic("angal.common.save.btn.key"));
			jButtonSave.setMaximumSize(new Dimension(ButtonWidth, ButtonHeight));
			jButtonSave.setIcon(new ImageIcon("rsc/icons/save_button.png"));
			jButtonSave.setHorizontalAlignment(SwingConstants.LEFT);
			jButtonSave.addActionListener(actionEvent -> {

				if (listSelected == null) {
					listSelected = lstArray.get(0);
				}

				if (insert) {
					RememberDates.setLastBillDate(billDate);             //to remember for next INSERT
					Bill newBill = new Bill(0,                        //Bill ID
							billDate,                                    //from calendar
							billDate,                                    //most recent payment
							true,                                   //is a List?
							listSelected,                                //List
							listSelected.getName(),                      //List name
							thisBill.isPatient(),                        //is a Patient?
							thisBill.isPatient() ?
									thisBill.getBillPatient() : null,    //Patient ID
							thisBill.isPatient() ?
									patientSelected.getName() :
									jTextFieldPatient.getText(),         //Patient Name
							paid ? "C" : "O",                            //CLOSED or OPEN
							total.doubleValue(),                         //Total
							balance.doubleValue(),                       //Balance
							user);                                       //User
					try {
						billManager.newBill(newBill, billItems, payItems);
						thisBill.setId(newBill.getId());
					} catch(OHServiceException ex) {
						OHServiceExceptionUtil.showMessages(ex, PatientBillEdit.this);
						return;
					}
					fireBillInserted(newBill);
					dispose();

				} else {
					Bill updateBill = new Bill(thisBill.getId(),         //Bill ID
							billDate,                                    //from calendar
							null,                                 //most recent payment
							true,                                  //is a List?
							listSelected,                                //List
							listSelected.getName(),                      //List name
							thisBill.isPatient(),                        //is a Patient?
							thisBill.isPatient() ?
									thisBill.getBillPatient() : null,    //Patient ID
							thisBill.isPatient() ?
									thisBill.getPatName() :
									jTextFieldPatient.getText(),         //Patient Name
							paid ? "C" : "O",                            //CLOSED or OPEN
							total.doubleValue(),                         //Total
							balance.doubleValue(),                       //Balance
							user);                                       //User

					try {
						BillBrowserManager billManager = Context.getApplicationContext().getBean(BillBrowserManager.class);
						billManager.updateBill(updateBill, billItems, payItems);
					} catch (OHServiceException ex) {
						OHServiceExceptionUtil.showMessages(ex, PatientBillEdit.this);
						return;
					}
					fireBillInserted(updateBill);
				}
				if (hasNewPayments()) {
					TxtPrinter.initialize();
					new GenericReportBill(thisBill.getId(), "PatientBillPayments", false, !TxtPrinter.PRINT_WITHOUT_ASK);
				}
				if (paid && GeneralData.RECEIPTPRINTER) {
					TxtPrinter.initialize();
					if (TxtPrinter.PRINT_AS_PAID) {
						new GenericReportBill(billID, GeneralData.PATIENTBILL, false, !TxtPrinter.PRINT_WITHOUT_ASK);
					}
				}
				dispose();
			});
		}
		return jButtonSave;
	}

	private boolean hasNewPayments() {
		return (insert && !payItems.isEmpty()) || (payItems.size() - payItemsSaved) > 0;
	}

	private JButton getJButtonPrintPayment() {
		if (jButtonPrintPayment == null) {
			jButtonPrintPayment = new JButton(MessageBundle.getMessage("angal.newbill.paymentreceipt.btn"));
			jButtonPrintPayment.setMnemonic(MessageBundle.getMnemonic("angal.newbill.paymentreceipt.btn.key"));
			jButtonPrintPayment.setMaximumSize(new Dimension(ButtonWidthPayment, ButtonHeight));
			jButtonPrintPayment.setHorizontalAlignment(SwingConstants.LEFT);
			jButtonPrintPayment.setIcon(new ImageIcon("rsc/icons/receipt_button.png"));
			jButtonPrintPayment.addActionListener(actionEvent -> {
				TxtPrinter.initialize();
				new GenericReportBill(thisBill.getId(), "PatientBillPayments", false, !TxtPrinter.PRINT_WITHOUT_ASK);
			});
		}
		if (insert) {
			jButtonPrintPayment.setEnabled(false);
		}
		return jButtonPrintPayment;
	}
	
	private JButton getJButtonPaid() {
		if (jButtonPaid == null) {
			jButtonPaid = new JButton(MessageBundle.getMessage("angal.newbill.paid.btn"));
			jButtonPaid.setMnemonic(MessageBundle.getMnemonic("angal.newbill.paid.btn.key"));
			jButtonPaid.setMaximumSize(new Dimension(ButtonWidth, ButtonHeight));
			jButtonPaid.setIcon(new ImageIcon("rsc/icons/ok_button.png"));
			jButtonPaid.setHorizontalAlignment(SwingConstants.LEFT);
			if (insert) {
				jButtonPaid.setEnabled(false);
			}
			jButtonPaid.addActionListener(actionEvent -> {

				GregorianCalendar datePay = new GregorianCalendar();

				Icon icon = new ImageIcon("rsc/icons/money_dialog.png"); //$NON-NLS-1$
				int ok = MessageDialog.yesNo(PatientBillEdit.this, icon,
						"angal.newbill.doyouwanttosetthecurrentbillaspaid.msg");
				if (ok == JOptionPane.NO_OPTION) {
					return;
				}

				if (balance.compareTo(new BigDecimal(0)) > 0) {
					if (billDate.before(today)) { //if Bill is in the past the user will be asked for PAID date

						icon = new ImageIcon("rsc/icons/calendar_dialog.png"); //$NON-NLS-1$

						CustomJDateChooser datePayChooser = new CustomJDateChooser(new Date());
						datePayChooser.setLocale(new Locale(GeneralData.LANGUAGE));
						datePayChooser.setDateFormatString("dd/MM/yy - HH:mm:ss"); //$NON-NLS-1$

				        int r = JOptionPane.showConfirmDialog(PatientBillEdit.this,
						        datePayChooser,
						        MessageBundle.getMessage("angal.newbill.dateofpayment.title"),
						        JOptionPane.OK_CANCEL_OPTION,
						        JOptionPane.PLAIN_MESSAGE,
						        icon);

				        if (r == JOptionPane.OK_OPTION) {
					        datePay.setTime(datePayChooser.getDate());
				        } else {
				            return;
				        }

					    if (isValidPaymentDate(datePay)) {
					        addPayment(datePay, balance.doubleValue());
				        }
					} else {
						datePay = new GregorianCalendar();
						addPayment(datePay, balance.doubleValue());
					}
				}
				paid = true;
				updateBalance();
				jButtonSave.doClick();
			});
		}
		return jButtonPaid;
	}
	
	private JButton getJButtonClose() {
		if (jButtonClose == null) {
			jButtonClose = new JButton(MessageBundle.getMessage("angal.common.close.btn"));
			jButtonClose.setMnemonic(MessageBundle.getMnemonic("angal.common.close.btn.key"));
			jButtonClose.setMaximumSize(new Dimension(ButtonWidth, ButtonHeight));
			jButtonClose.setIcon(new ImageIcon("rsc/icons/close_button.png"));
			jButtonClose.setHorizontalAlignment(SwingConstants.LEFT);
			jButtonClose.addActionListener(actionEvent -> {
				if (modified) {
					int ok = MessageDialog.yesNoCancel(PatientBillEdit.this, "angal.newbill.billhasbeenchangedwouldyouliketosavethechanges.msg");
					if (ok == JOptionPane.YES_OPTION) {
						jButtonSave.doClick();
					} else if (ok == JOptionPane.NO_OPTION) {
						dispose();
					}
				} else {
					dispose();
				}
			});
		}
		return jButtonClose;
	}

	private JButton getJButtonAddRefund() {
		if (jButtonAddRefund == null) {
			jButtonAddRefund = new JButton(MessageBundle.getMessage("angal.newbill.refund.btn"));
			jButtonAddRefund.setMnemonic(MessageBundle.getMnemonic("angal.newbill.refund.btn.key"));
			jButtonAddRefund.setMaximumSize(new Dimension(ButtonWidthPayment, ButtonHeight));
			jButtonAddRefund.setHorizontalAlignment(SwingConstants.LEFT);
			jButtonAddRefund.setIcon(new ImageIcon("rsc/icons/plus_button.png"));
			jButtonAddRefund.addActionListener(actionEvent -> {

				Icon icon = new ImageIcon("rsc/icons/money_dialog.png");
				BigDecimal amount = new BigDecimal(0);

				GregorianCalendar datePay = new GregorianCalendar();

				String quantity = (String) JOptionPane.showInputDialog(
	                    PatientBillEdit.this,
	                    MessageBundle.getMessage("angal.newbill.insertquantity.txt"),
	                    MessageBundle.getMessage("angal.common.quantity.txt"),
	                    JOptionPane.PLAIN_MESSAGE,
	                    icon,
	                    null,
	                    amount);
				if (quantity != null) {
					try {
						amount = new BigDecimal(quantity).negate();
						if (amount.equals(new BigDecimal(0))) {
							return;
						}
					} catch (Exception eee) {
						MessageDialog.error(PatientBillEdit.this, "angal.newbill.invalidquantitypleasetryagain.msg");
						return;
					}
				} else {
					return;
				}

				if (billDate.before(today)) { //if is a bill in the past the user will be asked for date of payment

					CustomJDateChooser datePayChooser = new CustomJDateChooser(new Date());
					datePayChooser.setLocale(new Locale(GeneralData.LANGUAGE));
					datePayChooser.setDateFormatString("dd/MM/yy - HH:mm:ss"); //$NON-NLS-1$

			        int r = JOptionPane.showConfirmDialog(PatientBillEdit.this,
					        datePayChooser,
					        MessageBundle.getMessage("angal.newbill.dateofpayment.title"),
					        JOptionPane.OK_CANCEL_OPTION,
					        JOptionPane.PLAIN_MESSAGE);

			        if (r == JOptionPane.OK_OPTION) {
				        datePay.setTime(datePayChooser.getDate());
			        } else {
			            return;
			        }

			        if (isValidPaymentDate(datePay)) {
				        addPayment(datePay, amount.doubleValue());
			        }
				} else {
					datePay = new GregorianCalendar();
					addPayment(datePay, amount.doubleValue());
				}
			});
		}
		return jButtonAddRefund;
	}

	private boolean isValidPaymentDate(GregorianCalendar datePay) {
		GregorianCalendar now = new GregorianCalendar();
		GregorianCalendar lastPay;
		if (!payItems.isEmpty()) {
			lastPay = payItems.get(payItems.size() - 1).getDate();
		} else {
			lastPay = billDate;
		}
		if (datePay.before(billDate)) {
			MessageDialog.error(PatientBillEdit.this, "angal.newbill.paymentmadebeforebilldate.msg");
			return false;
		} else if (datePay.before(lastPay)) {
			MessageDialog.error(PatientBillEdit.this, "angal.newbill.thedateisbeforethelastpayment.msg");
			return false;
		} else if (datePay.after(now)) {
			MessageDialog.error(PatientBillEdit.this, "angal.newbill.payementsinthefuturearenotallowed.msg");
			return false;
		}
		return true;
	}
	
	private JButton getJButtonAddPayment() {
		if (jButtonAddPayment == null) {
			jButtonAddPayment = new JButton(MessageBundle.getMessage("angal.newbill.payment.btn"));
			jButtonAddPayment.setMnemonic(MessageBundle.getMnemonic("angal.newbill.payment.btn.key"));
			jButtonAddPayment.setMaximumSize(new Dimension(ButtonWidthPayment, ButtonHeight));
			jButtonAddPayment.setHorizontalAlignment(SwingConstants.LEFT);
			jButtonAddPayment.setIcon(new ImageIcon("rsc/icons/plus_button.png"));
			jButtonAddPayment.addActionListener(actionEvent -> {

				Icon icon = new ImageIcon("rsc/icons/money_dialog.png");
				BigDecimal amount = new BigDecimal(0);

				GregorianCalendar datePay = new GregorianCalendar();

				String quantity = (String) JOptionPane.showInputDialog(
	                    PatientBillEdit.this,
	                    MessageBundle.getMessage("angal.newbill.insertquantity.txt"),
	                    MessageBundle.getMessage("angal.common.quantity.txt"),
	                    JOptionPane.PLAIN_MESSAGE,
	                    icon,
	                    null,
	                    amount);
				if (quantity != null) {
					try {
						amount = new BigDecimal(quantity);
						if (amount.equals(new BigDecimal(0))) {
							return;
						}
					} catch (Exception eee) {
						MessageDialog.error(PatientBillEdit.this, "angal.newbill.invalidquantitypleasetryagain.msg");
						return;
					}
				} else {
					return;
				}

				if (billDate.before(today)) { //if is a bill in the past the user will be asked for date of payment

					CustomJDateChooser datePayChooser = new CustomJDateChooser(new Date());
					datePayChooser.setLocale(new Locale(GeneralData.LANGUAGE));
					datePayChooser.setDateFormatString("dd/MM/yy - HH:mm:ss"); //$NON-NLS-1$

			        int r = JOptionPane.showConfirmDialog(PatientBillEdit.this,
					        datePayChooser,
					        MessageBundle.getMessage("angal.newbill.dateofpayment.title"),
					        JOptionPane.OK_CANCEL_OPTION,
					        JOptionPane.PLAIN_MESSAGE);

			        if (r == JOptionPane.OK_OPTION) {
				        datePay.setTime(datePayChooser.getDate());
			        } else {
			            return;
			        }

			        if (isValidPaymentDate(datePay)) {
				        addPayment(datePay, amount.doubleValue());
			        }
				} else {
					datePay = new GregorianCalendar();
					addPayment(datePay, amount.doubleValue());
				}
			});
		}
		return jButtonAddPayment;
	}

	private JButton getJButtonRemovePayment() {
		if (jButtonRemovePayment == null) {
			jButtonRemovePayment = new JButton(MessageBundle.getMessage("angal.newbill.removepayment.btn"));
			jButtonRemovePayment.setMnemonic(MessageBundle.getMnemonic("angal.newbill.removepayment.btn.key"));
			jButtonRemovePayment.setMaximumSize(new Dimension(ButtonWidthPayment, ButtonHeight));
			jButtonRemovePayment.setHorizontalAlignment(SwingConstants.LEFT);
			jButtonRemovePayment.setIcon(new ImageIcon("rsc/icons/delete_button.png"));
			jButtonRemovePayment.addActionListener(actionEvent -> {
				int row = jTablePayment.getSelectedRow();
				if (row > -1) {
					removePayment(row);
				}
			});
		}
		return jButtonRemovePayment;
	}
	
	private JButton getJButtonAddOther() {
		if (jButtonAddOther == null) {
			jButtonAddOther = new JButton(MessageBundle.getMessage("angal.newbill.other.btn"));
			jButtonAddOther.setMnemonic(MessageBundle.getMnemonic("angal.newbill.other.btn.key"));
			jButtonAddOther.setMaximumSize(new Dimension(ButtonWidthBill, ButtonHeight));
			jButtonAddOther.setHorizontalAlignment(SwingConstants.LEFT);
			jButtonAddOther.setIcon(new ImageIcon("rsc/icons/plus_button.png"));
			jButtonAddOther.addActionListener(actionEvent -> {

				boolean isPrice = true;

				HashMap<Integer,PricesOthers> othersHashMap = new HashMap<>();
				for (PricesOthers other : othPrices) {
				    othersHashMap.put(other.getId(), other);
			    }

				ArrayList<Price> othArray = new ArrayList<>();
				for (Price price : prcListArray) {
					if (price.getGroup().equals("OTH")) //$NON-NLS-1$
					{
						othArray.add(price);
					}
				}

				Icon icon = new ImageIcon("rsc/icons/plus_dialog.png");
				Price oth = (Price)JOptionPane.showInputDialog(
				                    PatientBillEdit.this,
				                    MessageBundle.getMessage("angal.newbill.pleaseselectanitem.txt"),
				                    MessageBundle.getMessage("angal.newbill.item.title"),
				                    JOptionPane.PLAIN_MESSAGE,
				                    icon,
				                    othArray.toArray(),
				                    ""); //$NON-NLS-1$

				if (oth != null) {
					if (othersHashMap.get(Integer.valueOf(oth.getItem())).isUndefined()) {
						icon = new ImageIcon("rsc/icons/money_dialog.png"); //$NON-NLS-1$
						String price = (String)JOptionPane.showInputDialog(
			                    PatientBillEdit.this,
			                    MessageBundle.getMessage("angal.newbill.howmuchisit.txt"),
			                    MessageBundle.getMessage("angal.common.undefined.txt"),
			                    JOptionPane.PLAIN_MESSAGE,
			                    icon,
			                    null,
								"0"); //$NON-NLS-1$
						try {
							if (price == null) {
								return;
							}
							double amount = Double.parseDouble(price);
							oth.setPrice(amount);
							isPrice = false;
						} catch (Exception eee) {
							MessageDialog.error(PatientBillEdit.this, "angal.newbill.invalidpricepleasetryagain.msg");
							return;
						}
					}
					if (othersHashMap.get(Integer.valueOf(oth.getItem())).isDischarge()) {
						double amount = oth.getPrice();
						oth.setPrice(-amount);
					}
					if (othersHashMap.get(Integer.valueOf(oth.getItem())).isDaily()) {
						int qty = 1;
						icon = new ImageIcon("rsc/icons/calendar_dialog.png"); //$NON-NLS-1$
						String quantity = (String) JOptionPane.showInputDialog(
			                    PatientBillEdit.this,
			                    MessageBundle.getMessage("angal.newbill.howmanydays.txt"),
			                    MessageBundle.getMessage("angal.newbill.days.title"),
			                    JOptionPane.PLAIN_MESSAGE,
			                    icon,
			                    null,
			                    qty);
						try {
							if (quantity == null || quantity.equals("")) {
								return;
							}
							qty = Integer.parseInt(quantity);
							addItem(oth, qty, isPrice);
						} catch (Exception eee) {
							MessageDialog.error(PatientBillEdit.this, "angal.newbill.invalidquantitypleasetryagain.msg");
						}
					} else {
						addItem(oth, 1, isPrice);
					}
				}
			});
		}
		return jButtonAddOther;
	}

	private JButton getJButtonAddExam() {
		if (jButtonAddExam == null) {
			jButtonAddExam = new JButton(MessageBundle.getMessage("angal.newbill.exam.btn"));
			jButtonAddExam.setMnemonic(MessageBundle.getMnemonic("angal.newbill.exam.btn.key"));
			jButtonAddExam.setMaximumSize(new Dimension(ButtonWidthBill, ButtonHeight));
			jButtonAddExam.setHorizontalAlignment(SwingConstants.LEFT);
			jButtonAddExam.setIcon(new ImageIcon("rsc/icons/plus_button.png"));
			jButtonAddExam.addActionListener(actionEvent -> {

				ArrayList<Price> exaArray = new ArrayList<>();
				for (Price price : prcListArray) {

					if (price.getGroup().equals("EXA")) //$NON-NLS-1$
					{
						exaArray.add(price);
					}
				}

				Icon icon = new ImageIcon("rsc/icons/exam_dialog.png"); //$NON-NLS-1$
				Price exa = (Price)JOptionPane.showInputDialog(
				                    PatientBillEdit.this,
				                    MessageBundle.getMessage("angal.newbill.selectanexam.txt"),
				                    MessageBundle.getMessage("angal.newbill.exam.title"),
				                    JOptionPane.PLAIN_MESSAGE,
				                    icon,
				                    exaArray.toArray(),
				                    ""); //$NON-NLS-1$
				addItem(exa, 1, true);
			});
		}
		return jButtonAddExam;
	}

	private JButton getJButtonAddOperation() {
		if (jButtonAddOperation == null) {
			jButtonAddOperation = new JButton(MessageBundle.getMessage("angal.newbill.operation.btn"));
			jButtonAddOperation.setMnemonic(MessageBundle.getMnemonic("angal.newbill.operation.btn.key"));
			jButtonAddOperation.setMaximumSize(new Dimension(ButtonWidthBill, ButtonHeight));
			jButtonAddOperation.setHorizontalAlignment(SwingConstants.LEFT);
			jButtonAddOperation.setIcon(new ImageIcon("rsc/icons/plus_button.png"));
			jButtonAddOperation.addActionListener(actionEvent -> {

				ArrayList<Price> opeArray = new ArrayList<>();
				for (Price price : prcListArray) {

					if (price.getGroup().equals("OPE")) //$NON-NLS-1$
					{
						opeArray.add(price);
					}
				}

				Icon icon = new ImageIcon("rsc/icons/operation_dialog.png"); //$NON-NLS-1$
				Price ope = (Price)JOptionPane.showInputDialog(
				                    PatientBillEdit.this,
				                    MessageBundle.getMessage("angal.newbill.selectanoperation.txt"),
				                    MessageBundle.getMessage("angal.newbill.operation.title"),
				                    JOptionPane.PLAIN_MESSAGE,
				                    icon,
				                    opeArray.toArray(),
				                    ""); //$NON-NLS-1$
				addItem(ope, 1, true);
			});
		}
		return jButtonAddOperation;
	}

	private JButton getJButtonAddMedical() {
		if (jButtonAddMedical == null) {
			jButtonAddMedical = new JButton(MessageBundle.getMessage("angal.newbill.medical.btn"));
			jButtonAddMedical.setMnemonic(MessageBundle.getMnemonic("angal.newbill.medical.btn"));
			jButtonAddMedical.setMaximumSize(new Dimension(ButtonWidthBill, ButtonHeight));
			jButtonAddMedical.setHorizontalAlignment(SwingConstants.LEFT);
			jButtonAddMedical.setIcon(new ImageIcon("rsc/icons/plus_button.png"));
			jButtonAddMedical.addActionListener(actionEvent -> {

				ArrayList<Price> medArray = new ArrayList<>();
				for (Price price : prcListArray) {

					if (price.getGroup().equals("MED")) //$NON-NLS-1$
					{
						medArray.add(price);
					}
				}

				Icon icon = new ImageIcon("rsc/icons/medical_dialog.png"); //$NON-NLS-1$
				Price med = (Price)JOptionPane.showInputDialog(
				                    PatientBillEdit.this,
				                    MessageBundle.getMessage("angal.newbill.selectamedical.txt"),
				                    MessageBundle.getMessage("angal.newbill.medical.title"),
				                    JOptionPane.PLAIN_MESSAGE,
				                    icon,
				                    medArray.toArray(),
				                    ""); //$NON-NLS-1$
				if (med != null) {
					int qty = 1;
					String quantity = (String) JOptionPane.showInputDialog(
		                    PatientBillEdit.this,
		                    MessageBundle.getMessage("angal.newbill.insertquantity.txt"),
		                    MessageBundle.getMessage("angal.common.quantity.txt"),
		                    JOptionPane.PLAIN_MESSAGE,
		                    icon,
		                    null,
		                    qty);
					try {
						if (quantity == null || quantity.equals("")) {
							return;
						}
						qty = Integer.parseInt(quantity);
						addItem(med, qty, true);
					} catch (Exception eee) {
						MessageDialog.error(PatientBillEdit.this,  "angal.newbill.invalidquantitypleasetryagain.msg");
					}
				}
			});
		}
		return jButtonAddMedical;
	}
	
	private JButton getJButtonAddCustom() {
		if (jButtonCustom == null) {
			jButtonCustom = new JButton(MessageBundle.getMessage("angal.newbill.custom.btn"));
			jButtonCustom.setMnemonic(MessageBundle.getMnemonic("angal.newbill.custom.btn.key"));
			jButtonCustom.setMaximumSize(new Dimension(ButtonWidthBill, ButtonHeight));
			jButtonCustom.setHorizontalAlignment(SwingConstants.LEFT);
			jButtonCustom.setIcon(new ImageIcon("rsc/icons/plus_button.png"));
			jButtonCustom.addActionListener(actionEvent -> {
				double amount;
				Icon icon = new ImageIcon("rsc/icons/custom_dialog.png"); //$NON-NLS-1$
				String desc = (String)JOptionPane.showInputDialog(
				                    PatientBillEdit.this,
				                    MessageBundle.getMessage("angal.newbill.chooseadescription.txt"),
				                    MessageBundle.getMessage("angal.newbill.customitem.title"),
				                    JOptionPane.PLAIN_MESSAGE,
				                    icon,
				                    null,
									MessageBundle.getMessage("angal.newbill.newdescription.txt"));
				if (desc == null || desc.equals("")) { //$NON-NLS-1$
					return;
				} else {
					icon = new ImageIcon("rsc/icons/money_dialog.png"); //$NON-NLS-1$
					String price = (String)JOptionPane.showInputDialog(
		                    PatientBillEdit.this,
		                    MessageBundle.getMessage("angal.newbill.howmuchisit.txt"),
		                    MessageBundle.getMessage("angal.newbill.customitem.title"),
		                    JOptionPane.PLAIN_MESSAGE,
		                    icon,
		                    null,
							"0"); //$NON-NLS-1$
					try {
						amount = Double.parseDouble(price);
					} catch (Exception eee) {
						MessageDialog.error(PatientBillEdit.this, "angal.newbill.invalidpricepleasetryagain.msg");
						return;
					}

				}

				try {
					BillItems newItem = new BillItems(0,
							billManager.getBill(billID),
							false,
							"", //$NON-NLS-1$
							desc,
							amount,
							1);
					addItem(newItem);
				} catch (OHServiceException ohServiceException) {
					MessageDialog.showExceptions(ohServiceException);
				}
			});
		}
		return jButtonCustom;
	}
	
	private JButton getJButtonRemoveItem() {
		if (jButtonRemoveItem == null) {
			jButtonRemoveItem = new JButton(MessageBundle.getMessage("angal.newbill.removeitem.btn"));
			jButtonRemoveItem.setMnemonic(MessageBundle.getMnemonic("angal.newbill.removeitem.btn.key"));
			jButtonRemoveItem.setMaximumSize(new Dimension(ButtonWidthBill, ButtonHeight));
			jButtonRemoveItem.setHorizontalAlignment(SwingConstants.LEFT);
			jButtonRemoveItem.setIcon(new ImageIcon("rsc/icons/delete_button.png"));
			jButtonRemoveItem.addActionListener(actionEvent -> {
				int row = jTableBill.getSelectedRow();
				if (row > -1) {
					removeItem(row);
				}
			});
		}
		return jButtonRemoveItem;
	}
	
	private void updateTotal() { //only positive items make the bill's total
		total = new BigDecimal(0);
		for (BillItems item : billItems) {
			double amount = item.getItemAmount();
			if (amount > 0) {
				BigDecimal itemAmount = new BigDecimal(Double.toString(amount));
				total = total.add(itemAmount.multiply(new BigDecimal(item.getItemQuantity())));
			}
		}
	}
	
	private void updateBigTotal() { //the big total (to pay) is made by all items
		bigTotal = new BigDecimal(0);
		for (BillItems item : billItems) {
			BigDecimal itemAmount = new BigDecimal(Double.toString(item.getItemAmount()));
			bigTotal = bigTotal.add(itemAmount.multiply(new BigDecimal(item.getItemQuantity())));			
		}
	}
	
	private void updateBalance() { //the balance is what remaining after payments
		balance = new BigDecimal(0);
		BigDecimal payments = new BigDecimal(0);
		for (BillPayments pay : payItems) {
			BigDecimal payAmount = new BigDecimal(Double.toString(pay.getAmount()));
			payments = payments.add(payAmount); 
		}
		balance = bigTotal.subtract(payments);
		if (jButtonPaid != null) {
			jButtonPaid.setEnabled(balance.compareTo(new BigDecimal(0)) >= 0);
		}
		if (jButtonBalance != null) {
			jButtonBalance.setEnabled(balance.compareTo(new BigDecimal(0)) >= 0);
		}
	}

	private void addItem(Price prc, int qty, boolean isPrice) {
		if (prc != null) {
			double amount = prc.getPrice();
			try {
				BillItems item = new BillItems(0, 
						billManager.getBill(billID), 
						isPrice, 
						prc.getGroup()+prc.getItem(),
						prc.getDesc(),
						amount,
						qty);
				billItems.add(item);
			} catch(OHServiceException e) {
				OHServiceExceptionUtil.showMessages(e, PatientBillEdit.this);
			}
			modified = true;
			jTableBill.updateUI();
			updateTotals();
		}
	}
	
	private void updateUI() {
		
		jCalendarDate.setDate(thisBill.getDate().getTime());
		jTextFieldPatient.setText(patientSelected.getName());
		jTextFieldPatient.setEditable(false);
		jButtonPickPatient.setText(MessageBundle.getMessage("angal.newbill.changepatient.btn"));
		jButtonPickPatient.setToolTipText(MessageBundle.getMessage("angal.newbill.changethepatientassociatedwiththisbill.tooltip"));
		jButtonTrashPatient.setEnabled(true);
		jTableBill.updateUI();
		jTablePayment.updateUI();
		updateTotals();
	}
	
	private void updateTotals() {
		updateTotal();
		updateBigTotal();
		updateBalance();
		jTableTotal.getModel().setValueAt(total, 0, 2);
		jTableBigTotal.getModel().setValueAt(bigTotal, 0, 2);
		jTableBalance.getModel().setValueAt(balance, 0, 2);
	}
	
	private void addItem(BillItems item) {
		if (item != null) {
			billItems.add(item);
			modified = true;
			jTableBill.updateUI();
			updateTotals();
		}
	}
	
	private void addPayment(GregorianCalendar datePay, double qty) {
		if (qty != 0) {
			try {
				BillPayments pay = new BillPayments(0,
						billManager.getBill(billID),
						datePay,
						qty,
						user);
				payItems.add(pay);
			} catch (OHServiceException e) {
				OHServiceExceptionUtil.showMessages(e, PatientBillEdit.this);
			}
			modified = true;
			Collections.sort(payItems);
			jTablePayment.updateUI();
			updateBalance();
			jTableBalance.getModel().setValueAt(balance, 0, 2);
		}
	}
	
	private void removeItem(int row) {
		if (row != -1 && row >= billItemsSaved) {
			billItems.remove(row);
			jTableBill.updateUI();
			jTableBill.clearSelection();
			updateTotals();
		} else {
			MessageDialog.error(null, "angal.newbill.youcannotdeletealreadysaveditems.msg");
		}
	}
	
	private void removePayment(int row) {
		if (row != -1 && row >= payItemsSaved) {
			payItems.remove(row);
			jTablePayment.updateUI();
			jTablePayment.clearSelection();
			updateTotals();
		} else {
			MessageDialog.error(null, "angal.newbill.youcannotdeletepastpayments.msg");
		}
	}
	
	public class BillTableModel implements TableModel {
		
		public BillTableModel() {
			
			HashMap<String,Price> priceHashTable = new HashMap<>();
			prcListArray = new ArrayList<>();
			//billItems = new ArrayList<BillItems>();
			
			/*
			 * Select the prices of the selected list.
			 * If no price list is selected (new bill) the first one is taken.
			 */
			if (listSelected == null) {
				listSelected = lstArray.get(0);
			}
			for (Price price : prcArray) {
				if (price.getList().getId() == listSelected.getId()) {
					prcListArray.add(price);
				}
		    }
			
			/*
			 * Create a hashTable with the selected prices.
			 */
			for (Price price : prcListArray) {
				priceHashTable.put(price.getList().getId()+
  					  price.getGroup()+
  					  price.getItem(), price);
		    }
			
			/*
			 * Updates the items in the bill.
			 */
		    for (BillItems item : billItems) {
				
				if (item.isPrice()) {
					Price p = priceHashTable.get(listSelected.getId()+item.getPriceID());
					item.setItemDescription(p.getDesc());
					item.setItemAmount(p.getPrice());
				}
			}
			
		    /*
		     * Updates the totals.
		     */
		    updateTotal();
		    updateBigTotal();
			updateBalance();
		}
		
		@Override
		public Class<?> getColumnClass(int i) {
			return billClasses[i].getClass();
		}

		
		@Override
		public int getColumnCount() {
			return billClasses.length;
		}
		
		@Override
		public int getRowCount() {
			if (billItems == null) {
				return 0;
			}
			return billItems.size();
		}
		
		@Override
		public Object getValueAt(int r, int c) {
			BillItems item = billItems.get(r);
			if (c == -1) {
				return item;
			}
			if (c == 0) {
				return item.getItemDescription();
			}
			if (c == 1) {
				return item.getItemQuantity(); 
			}
			if (c == 2) {
				BigDecimal qty = new BigDecimal(item.getItemQuantity());
				BigDecimal amount = new BigDecimal(Double.toString(item.getItemAmount()));
				return amount.multiply(qty).doubleValue();
			}
			return null;
		}
		
		@Override
		public boolean isCellEditable(int r, int c) {
			return c == 1;
		}
		
		@Override
		public void setValueAt(Object item, int r, int c) {
			//if (c == 1) billItems.get(r).setItemQuantity((Integer)item);
		}

		@Override
		public void addTableModelListener(TableModelListener l) {
		}

		@Override
		public String getColumnName(int columnIndex) {
			return billColumnNames[columnIndex];
		}

		@Override
		public void removeTableModelListener(TableModelListener l) {
		}

	}

	public class PaymentTableModel implements TableModel {
		
		public PaymentTableModel() {
			updateBalance();
		}
		
		@Override
		public void addTableModelListener(TableModelListener l) {

		}
		
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return paymentClasses[columnIndex].getClass();
		}
		
		@Override
		public int getColumnCount() {
			return paymentClasses.length;
		}
		
		@Override
		public String getColumnName(int columnIndex) {
			return null;
		}
		
		@Override
		public int getRowCount() {
			return payItems.size();
		}
		
		@Override
		public Object getValueAt(int r, int c) {
			if (c == -1) {
				return payItems.get(r);
			}
			if (c == 0) {
				return formatDateTime(payItems.get(r).getDate());
			}
			if (c == 1) {
				return payItems.get(r).getAmount(); 
			}
			return null;
		}
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}
		
		@Override
		public void removeTableModelListener(TableModelListener l) {
		}
		
		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
		}
	}

	public String formatDate(GregorianCalendar time) {
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");  //$NON-NLS-1$
		return format.format(time.getTime());
	}
	
	public String formatDateTime(GregorianCalendar time) {
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");  //$NON-NLS-1$
		return format.format(time.getTime());
	}
	
	public boolean isSameDay(GregorianCalendar billDate, GregorianCalendar today) {
		return (billDate.get(Calendar.YEAR) == today.get(Calendar.YEAR)) &&
			   (billDate.get(Calendar.MONTH) == today.get(Calendar.MONTH)) &&
			   (billDate.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH));
	}
	
	public void checkIfsameMonth() {
		if (!insert) {
			//GregorianCalendar thisday = TimeTools.getServerDateTime();
			GregorianCalendar thisday = new GregorianCalendar();
			GregorianCalendar billDate = thisBill.getDate();
			int thisMonth = thisday.get(Calendar.MONTH);
			int billMonth = billDate.get(Calendar.MONTH);
			int thisYear = thisday.get(Calendar.YEAR);
			int billBillYear = billDate.get(Calendar.YEAR);
			if (thisYear>billBillYear || thisMonth>billMonth) {
				jButtonAddMedical.setEnabled(false);
				jButtonAddOperation.setEnabled(false);
				jButtonAddExam.setEnabled(false);
				jButtonAddOther.setEnabled(false);
				jButtonCustom.setEnabled(false);
				jButtonRemoveItem.setEnabled(false);
				//jTextFieldSearch.setEnabled(false);
				//jTextFieldDescription.setEnabled(false);
				//jTextFieldQty.setEnabled(false);
				//jTextFieldPrice.setEnabled(false);
				//jButtonAddPrescription.setEnabled(false);
				jCalendarDate.grabFocus(); 
			}
		}
	}
}
